package cpath.converter.internal;

import org.biopax.paxtools.controller.ModelUtils.RelationshipType;
import org.biopax.paxtools.model.level3.BioSource;
import org.biopax.paxtools.model.level3.ModificationFeature;
import org.biopax.paxtools.model.level3.PositionStatusType;
import org.biopax.paxtools.model.level3.ProteinReference;
import org.biopax.paxtools.model.level3.RelationshipTypeVocabulary;
import org.biopax.paxtools.model.level3.RelationshipXref;
import org.biopax.paxtools.model.level3.SequenceInterval;
import org.biopax.paxtools.model.level3.SequenceModificationVocabulary;
import org.biopax.paxtools.model.level3.SequenceSite;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.validator.utils.Normalizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cpath.importer.Converter;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;

/**
 * Implementation of {@link Converter} interface for UniProt data.
 * 
 * @see http://web.expasy.org/docs/userman.html
 */
final class UniprotConverterImpl extends BaseConverterImpl {

	// logger
    private static final Log log = LogFactory.getLog(UniprotConverterImpl.class);
       
    /**
     * Constructor
     */
	UniprotConverterImpl() {
	}

	/**
	 * (non-Javadoc>
	 * @see cpath.importer.Converter#convert(java.io.InputStream)
	 */
	@Override
	public void convert(final InputStream is) {

		// ref to reader here so
		// we can close in finally clause
        InputStreamReader reader= null;

        try {
            reader = new InputStreamReader(is, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(reader);
            String line = bufferedReader.readLine();
            final HashMap<String, StringBuilder> dataElements = new HashMap<String, StringBuilder>();
            if (log.isInfoEnabled()) {
            	log.info("convert(), starting to read data...");
			}
            long linesReadSoFar = 0;
            while (line != null) {
            	linesReadSoFar++;
                if (line.startsWith ("//")) //reached the end of a Uniprot entry
                {
					// grab properties from the map and prepare for parsing
                    String deField = ((StringBuilder) dataElements.get("DE")).toString();
                    String organismName = ((StringBuilder) dataElements.get("OS")).toString(); //mostly occurs once per entry 
                    String organismTaxId = ((StringBuilder) dataElements.get("OX")).toString(); //occurs once per entry 
                    StringBuilder comments = (StringBuilder) dataElements.get("CC");
                    StringBuilder geneName = (StringBuilder) dataElements.get("GN");
                    String acNames = ((StringBuilder) dataElements.get("AC")).toString();
                    StringBuilder xrefs = (StringBuilder) dataElements.get("DR");
                    String idParts[] = ((StringBuilder) dataElements.get("ID")).toString().split("\\s+");
                    StringBuilder sq = (StringBuilder) dataElements.get("SQ"); //SEQUENCE SUMMARY
                    StringBuilder sequence = (StringBuilder) dataElements.get("  "); //SEQUENCE
                    StringBuilder features = (StringBuilder) dataElements.get("FT"); //strict format in 6-75 char in each FT line!
                    
                    ProteinReference proteinReference = newUniProtWithXrefs(idParts[0], acNames, xrefs);
                    
                    setNameAndSynonyms(proteinReference, deField);
                    
                    setOrganism(organismName, organismTaxId, proteinReference);
                    
                    String geneSyns = null;
                    if (geneName != null) {
                        geneSyns= setGeneSymbolAndSynonyms(geneName.toString(), proteinReference);
                    }
                    
                    if (comments != null) {
                        setComments (comments.toString(), geneSyns, proteinReference);
                    }

                    if(sequence != null) { //set sequence (remove spaces)
                    	String seq = sequence.toString().replaceAll("\\s", "");
                    	proteinReference.setSequence(seq);
                    	proteinReference.addComment(sq.toString()); //sequence summary
                    }
                    
                    //create modified residue features
                    if(features != null)
                    	createModResFeatures(features.toString(), proteinReference);
                    
                    // debug: write the one-protein-reference model
                    if(log.isDebugEnabled()) {
                    	log.debug("convert(). so far line# " + linesReadSoFar);
                    }
                    
                    dataElements.clear();
                }
				else { //continue read and collect current Uniprot entry lines
					/* The two-character line-type code that begins each line is
					 * always followed by three blanks, so that the actual 
					 * information begins with the sixth character.
					 */
                    String key = line.substring (0, 2);
                    String data = line.substring(5);
                    if (data.startsWith("-------") ||
                            data.startsWith("Copyrighted") ||
                            data.startsWith("Distributed")) {
                        //  do nothing here...
                    } else {
                        if (dataElements.containsKey(key)) {
                            StringBuilder existingData = (StringBuilder) dataElements.get(key);
                            existingData.append(data); //TODO (just remove " ") - test it!
                        }
						else {
                            dataElements.put(key, new StringBuilder (data));
                        }
                    }
                }
                line = bufferedReader.readLine();
            }
        }
		catch(IOException e) {
			log.error(e);
		}
		finally {
			if (log.isDebugEnabled()) {
				log.debug("convert(), closing reader.");
			}
            if (reader != null) {
				try {
					reader.close();
				}
				catch (Exception e) {
					// ignore
				}
            }
        }
        
		if (log.isInfoEnabled()) {
			log.info("convert(), exiting.");
		}
    }

	
	/*
	 * Sets name and synonyms on protein reference.
	 *
	 * @param proteinReference ProteinReference
	 * @param deField String
	 */
    private void setNameAndSynonyms (ProteinReference proteinReference, String deField) {
        //  With the latest UNIPROT Export, the DE Line contains multiple fields.
        //  For example:
        //  DE   RecName: Full=14-3-3 protein beta/alpha;
        //  DE   AltName: Full=Protein kinase C inhibitor protein 1;
        //  DE            Short=KCIP-1;
        //  DE   AltName: Full=Protein 1054;
        //  We only want DE  RecName: Full
        if (deField != null && deField.length() > 0) {
            String fields[] = deField.split(";");
            for (String field: fields) {
                String parts[] = field.split("=");
                if (parts.length == 2) {
                    String fieldName = parts[0]; //no trim() required here!
                    String fieldValue = parts[1].trim();
                    if ("RecName: Full".equals(fieldName)) {
						proteinReference.setStandardName(fieldValue);
                    }
					else {
						proteinReference.addName(fieldValue);
                    }
                    //TODO Do we really want to add all names from Contains: and Includes: sections?
                }
            }
        }
    }

    /**
     * Sets the Current Organism Information.
	 *
	 * @param organismName String
	 * @param organismTaxId String
	 * @param proteinReference ProteinReference
     */
    private void setOrganism(String organismName, String organismTaxId, ProteinReference proteinReference) {
        String parts[] = organismTaxId.replaceAll(";", "").split("=");
        String taxId = parts[1];
        parts = organismName.split("\\("); // - by first occurrence of '('
        String name = parts[0].trim();
		BioSource bioSource = getBioSource(taxId, name);
		proteinReference.setOrganism(bioSource);
    }

    /**
     * Sets Multiple Comments.
	 *
	 * @param comments String
	 * @param geneSynonyms String
	 * @param proteinReference ProteinReference
     */
    private void setComments (String comments, String geneSynonyms, 
    		ProteinReference proteinReference) 
    {
        String commentParts[] = comments.split("-!- ");
        StringBuilder reducedComments = new StringBuilder();
        for (int i=0; i<commentParts.length; i++) {
            String currentComment = commentParts[i];
            //  Filter out the Interaction comments.
            //  We don't want these, as cPath itself will contain the interactions.
            if (!currentComment.startsWith("INTERACTION")) {
                currentComment = currentComment.replaceAll("     ", " ");
                reducedComments.append (currentComment);
            }
        }
        if (geneSynonyms != null && geneSynonyms.length() > 0) {
            reducedComments.append (" GENE SYNONYMS:" + geneSynonyms + ".");
        }
        if (reducedComments.length() > 0) {
            reducedComments.append (" COPYRIGHT:  Protein annotation is derived from the "
                    + "UniProt Consortium (http://www.uniprot.org/).  Distributed under "
                    + "the Creative Commons Attribution-NoDerivs License.");
        }
        
		proteinReference.addComment(reducedComments.toString());
    }


    /**
     * Sets Multiple Types of XRefs, e.g. Entrez Gene ID and RefSeq.
	 *
	 * @param dbRefs String (concatenated with a space 'DR' lines)
	 * @param proteinReference
     */
    private void setXRefs (String dbRefs, ProteinReference proteinReference) {
        String xrefList[] = dbRefs.split("\\."); 
        // - there are no '.' in the string array values anymore

        for (int i=0; i<xrefList.length; i++) {
            String xref = xrefList[i].trim();
            if (xref.startsWith("GeneID")) {
                String parts[] = xref.split(";");
                String entrezGeneId = parts[1];
                setRelationshipXRef("Entrez Gene", entrezGeneId, proteinReference, RelationshipType.GENE);
            }
			else if (xref.startsWith("RefSeq")) {
                String parts[] = xref.split(";");
                String refSeqId = parts[1];
                setRelationshipXRef("RefSeq", refSeqId, proteinReference, RelationshipType.SEQUENCE);
            }
			else if (xref.startsWith("HGNC") 
					|| xref.startsWith("MGI") 
						|| xref.startsWith("RGD")) {
				//TODO this xref is created only for hs, mm, rn species; do we need this at all?..
                String parts[] = xref.split(";");
                String db = parts[0].trim();
                String id = parts[1];
                setRelationshipXRef(db, id, proteinReference, RelationshipType.GENE);
            }
        }
    }

    
    /**
     * Sets the HUGO Gene Symbol and Synonyms.
	 *
	 * @param proteinReference ProteinReference
     */
    private String setGeneSymbolAndSynonyms(String geneName, ProteinReference proteinReference) 
    {
        StringBuilder synBuffer = new StringBuilder();
        String parts[] = geneName.split(";");
        for (int i=0; i<parts.length; i++) {
            String subParts[] = parts[i].split("=");
            // add, e.g., HUGO Gene Name to protein names
            if (subParts[0].trim().equals("Name")) {
            	proteinReference.addName(subParts[1]);
            	synBuffer.append(subParts[1]);
            }
			else if (subParts[0].trim().equals("Synonyms")) {
                String synList[] = subParts[1].split(",");
                for (int j=0; j<synList.length; j++) {
                    String currentSynonym = synList[j].trim();
                    synBuffer.append(" " + currentSynonym);
                }
            }
        }
        return synBuffer.toString();
    }

    /**
     * Sets Relationship XRefs.
	 *
     * @param dbName String
     * @param id String
     * @param proteinReference ProteinReference
     * @param relationshipType names from {@link RelationshipType} enum.
     */
    private void setRelationshipXRef(String dbName, 
    	String id, ProteinReference proteinReference, RelationshipType relationshipType) 
    {
        id = id.trim();
		String rdfId = Normalizer.uri(model.getXmlBase(), dbName, id, RelationshipXref.class);
		RelationshipXref rXRef = (RelationshipXref) model.getByID(rdfId);
		if (rXRef == null) {
			rXRef = (RelationshipXref) model.addNew(RelationshipXref.class,	rdfId);
			rXRef.setDb(dbName);
			rXRef.setId(id);
		}
		
		//find/create a special relationship CV
		String relCvId = Normalizer.uri(model.getXmlBase(),	null, relationshipType.name(), RelationshipTypeVocabulary.class);
		RelationshipTypeVocabulary relCv = (RelationshipTypeVocabulary) model.getByID(relCvId);
		if(relCv == null) {
			relCv = model.addNew(RelationshipTypeVocabulary.class, relCvId);
			relCv.addTerm(relationshipType.name());
		}
		rXRef.setRelationshipType(relCv);
		proteinReference.addXref(rXRef);
    }

    /**
     * Sets Unification XRefs.
	 * 
	 * @param dbName String
	 * @param id tring
	 * @param proteinReference ProteinReference
     */
    private void setUnificationXRef(String dbName, String id, ProteinReference proteinReference) {
        id = id.trim();
		String rdfId = Normalizer.uri(model.getXmlBase(), dbName, id, UnificationXref.class);
		
		UnificationXref rXRef = (UnificationXref) model.getByID(rdfId);
		if (rXRef == null) {
			rXRef = (UnificationXref) model.addNew(UnificationXref.class, rdfId);
			rXRef.setDb(dbName);
			rXRef.setId(id);
		}
		
		proteinReference.addXref(rXRef);
    }

	/**
	 * Gets a protein (or ProteinReference in L3);
	 * set its RDFId and all the xrefs.
	 *
	 * @param shortName
	 * @param accessions AC field values
	 * @param dbRefs DR field values
	 * @return ProteinReference
	 */
	private ProteinReference newUniProtWithXrefs(String shortName, String accessions, StringBuilder dbRefs) 
	{	
		// accession numbers as array
		String acList[] = accessions.split(";");
		// the first one, primary id, becomes the RDFId
		String id = "http://identifiers.org/uniprot/" + acList[0].trim().toUpperCase();
		// create new pr
		ProteinReference proteinReference = model.addNew(ProteinReference.class, id);
		proteinReference.setDisplayName(shortName);

		// add all unification xrefs
		for (String acEntry : acList) {
			String ac = acEntry.trim();
			setUnificationXRef("uniprot", ac, proteinReference);
		}
		
		// add other xrefs
        if (dbRefs != null) {
            setXRefs(dbRefs.toString(), proteinReference);
        }
		
		return proteinReference;
	}

	/**
	 * Gets a biosource
	 *
	 * @param taxId String
	 * @param name String
	 * @return BioSource
	 */
	private BioSource getBioSource(String taxId, String name) 
	{
		// check taxonomy ID is integer value
		Integer taxonomy = null;
		try {
			taxonomy = Integer.valueOf(taxId);
		} catch (NumberFormatException e) {
			throw new RuntimeException("Faild to convert " + taxId
					+ " into integer taxonomy ID!", e);
		}

		BioSource toReturn = null;

		// check the organism was previously used, re-use it
		if(taxonomy==null || taxonomy <= 0) {
			throw new RuntimeException("Illegal taxonomy ID: " + taxId);
		} else {
			String uri = "http://identifiers.org/taxonomy/" + taxonomy;
			if (model.containsID(uri)) {
				toReturn = (BioSource) model.getByID(uri);
			} else {
				toReturn = (BioSource) model
						.addNew(BioSource.class, uri);
				toReturn.setStandardName(name);
				toReturn.setDisplayName(name);
				UnificationXref taxonXref = (UnificationXref) model
					.addNew(UnificationXref.class, Normalizer
						.uri(model.getXmlBase(), "TAXONOMY", taxId, UnificationXref.class));
				taxonXref.setDb("taxonomy");
				taxonXref.setId(taxId);
				// TODO update when taxonXref is removed (deprecated property)
				toReturn.addXref((UnificationXref) taxonXref);
			}
		}
		return toReturn;
	}

	
	/*
	 * Parses only "FT   MOD_RES   N    M   Term..." lines and creates modification features and sites
	 * (the "MOD_RES   N    M   Term." part always has no more than 70 chars)
	 * 
	 */
	private void createModResFeatures(final String features, 
			final ProteinReference pr) 
	{
		// using a special "not greedy" regex!
        Pattern pattern = Pattern.compile("MOD_RES.+?\\.");
        Matcher matcher = pattern.matcher(features);
        int mfIndex = 0;
        while(matcher.find()) {
        	String ftContent = matcher.group();
			String what = ftContent.substring(29, ftContent.length()-1); //the term without final '.'
			// split the result by ';' (e.g., it might now look like "Phosphothreonine; by CaMK4") 
			// to extract the modification type and create the standard PSI-MOD synonym; 
			String[] terms = what.toString().split(";");
			final String mod = terms[0];
			final String modTerm = "MOD_RES " + mod; //PSI-MOD term synonym
			
			// Create the feature with CV and location -
			mfIndex++;
			String uri = Normalizer.uri(model.getXmlBase(), 
					null, pr.getDisplayName() + "_" + mfIndex, ModificationFeature.class);
			ModificationFeature modificationFeature = model.addNew(ModificationFeature.class, uri);
			modificationFeature.addComment(ftContent);
			// get or create a new PSI-MOD SequenceModificationVocabulary, which 
			// - can be shared by many protein references we create
			uri = Normalizer.uri(model.getXmlBase(), "MOD", mod, SequenceModificationVocabulary.class);
			// so, let's check if it exists in the temp. or target model:
			SequenceModificationVocabulary cv = (SequenceModificationVocabulary) model.getByID(uri);
			if(cv == null)
				cv = (SequenceModificationVocabulary) model.getByID(uri);
			if(cv == null) {
				// create a new SequenceModificationVocabulary
				cv = model.addNew(SequenceModificationVocabulary.class, uri);
				cv.addTerm(modTerm);
				// TODO normalize to the preferred term and attach uni.xref (e.g., during the Merge, requires Ontology Manager!)
			}
			modificationFeature.setModificationType(cv);
			
			// create feature location (site or interval)
			final int start = Integer.parseInt(ftContent.substring(9, 15).trim());
			final int end = Integer.parseInt(ftContent.substring(16, 22).trim());
			// so, let's check if the site exists in the temp. model
			String idPart = pr.getDisplayName() + //e.g., CALM_HUMAN - from the ID line 
								"_" + start;
			uri = Normalizer.uri(model.getXmlBase(), null, idPart, SequenceSite.class);			
						
			SequenceSite startSite = (SequenceSite) model.getByID(uri);
			if(startSite == null) {
				startSite = model.addNew(SequenceSite.class, uri);
				startSite.setPositionStatus(PositionStatusType.EQUAL);
				startSite.setSequencePosition(start);
			}
			
			if(start == end) {
				modificationFeature.setFeatureLocation(startSite);
				//TODO modificationFeature.setFeatureLocationType(regionVocabulary?);
			} else {
				//create the second site (end) and sequence interval -
				idPart = pr.getDisplayName() + //e.g., CALM_HUMAN - from the ID line
							"_" + end;
				uri = Normalizer.uri(model.getXmlBase(), null, idPart, SequenceSite.class);
					
				SequenceSite endSite = (SequenceSite) model.getByID(uri);
				if(endSite == null) {
					endSite = model.addNew(SequenceSite.class, uri);
					endSite.setPositionStatus(PositionStatusType.EQUAL);
					endSite.setSequencePosition(end);
				}
				
				idPart = pr.getDisplayName() + 	"_" + start + "_" + end;
				uri = Normalizer.uri(model.getXmlBase(), null, idPart, SequenceInterval.class);
						
				SequenceInterval sequenceInterval = (SequenceInterval) model.getByID(uri);
				if(sequenceInterval == null) {
					sequenceInterval = model.addNew(SequenceInterval.class, uri);
					sequenceInterval.setSequenceIntervalBegin(startSite);
					sequenceInterval.setSequenceIntervalEnd(endSite);
				}
				
				modificationFeature.setFeatureLocation(sequenceInterval);
			}
			
			pr.addEntityFeature(modificationFeature);
        }
		
	}
}
