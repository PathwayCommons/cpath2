package cpath.converter.internal;

import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpath.importer.Converter;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;

/**
 * Implementation of {@link Converter} interface for UniProt data.
 * 
 * @see http://web.expasy.org/docs/userman.html
 * @see http://www.uniprot.org/faq/30
 */
final class UniprotConverterImpl extends BaseConverterImpl {

    private static final Logger log = LoggerFactory.getLogger(UniprotConverterImpl.class);
       
    /**
     * Constructor
     */
	UniprotConverterImpl() {}


	public void convert(InputStream is, OutputStream os) {
		// ref to reader here so
		// we can close in finally clause
        InputStreamReader reader= null;
        Model model = BioPAXLevel.L3.getDefaultFactory().createModel();
        model.setXmlBase(xmlBase);
        
        try {
            reader = new InputStreamReader(is, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(reader);
            String line = bufferedReader.readLine();
            final HashMap<String, StringBuilder> dataElements = new HashMap<String, StringBuilder>();
           	log.info("convert(), starting to read data...");
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
                    StringBuilder features = (StringBuilder) dataElements.get("FT"); //strict format in 6-75 char in each FT line
                    
                    ProteinReference proteinReference = newUniProtWithXrefs(idParts[0], acNames, model);
                    
            		// add some external xrefs from DR fileds
                    if (xrefs != null) {
                        setXRefsFromDRs(xrefs.toString(), proteinReference, model);
                    }
                          
                    setNameAndSynonyms(proteinReference, deField);
                    
                    setOrganism(organismName, organismTaxId, proteinReference, model);
                    
                    // GN gene symbols - to PR names and rel. xrefs
                    if (geneName != null) {
                    	Collection<String> geneNames = getGeneSymbols(geneName.toString(), proteinReference);
                        // always use "HGNC Symbol" for rel. xrefs, despite it can be from MGI, RGD,.. (these are coordinated by HGNC)
                    	// (cannot do this in setXRefsFromDRs: no gene synonyms there, and organism specific db names like MGI)
                        for (String symbol : geneNames) {
                        	// also add Gene Names to PR names (can be >1 due to isoforms)
                        	proteinReference.addName(symbol);
                        	setRelationshipXRef("HGNC SYMBOL", symbol, proteinReference, null, model);
                        }
                    }
                    
                    //synonyms from GN
                    if (geneName != null) {
                    	Collection<String> geneSyns = getGeneSynonyms(geneName.toString(), proteinReference);
                        for (String symbol : geneSyns) {
                        	// also add gene synonyms to PR names but do not create xrefs 
                        	// (these are not necessarily official HGNC Symbols and ambiguous)
                        	proteinReference.addName(symbol);
                        }
                    }
                                      
                    // add some info from CC fields to BioPAX comments
                    if (comments != null) {
                        setComments (comments.toString(), proteinReference);
                    }

// won't store canonical sequences (in practice, it does not help and may even mislead: 
// in fact, one usually needs to know an isoform sequence (variant) and its version exactly)
//                    if(sequence != null) { //set sequence (remove spaces)
//                    	String seq = sequence.toString().replaceAll("\\s", "");
//                    	proteinReference.setSequence(seq);
//                    	proteinReference.addComment(sq.toString()); //sequence summary
//                    }
                    
                    //create modified residue features
                    if(features != null)
                    	createModResFeatures(features.toString(), proteinReference, model);
                    
                    // debug: write the one-protein-reference model
                    log.debug("convert(). so far line# " + linesReadSoFar);
                    
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
                            if("DR".equals(key))
                            	//EOLs do matter for correct splitting into records; adding back (because bufferedReader.readLine() removed it)
                            	existingData.append(data).append('\n'); 
                            else
                            	existingData.append(data);
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
			throw new RuntimeException("Failed to convert UniProt data to BioPAX", e);
		}
		finally {
			log.debug("convert(), closing reader.");
            if (reader != null) {
				try {
					reader.close();
				}
				catch (Exception e) {
					// ignore
				}
            }
        }       

        log.info("convert(), repairing.");
        model.repair();
        
        log.info("convert(), writing.");
        new SimpleIOHandler(BioPAXLevel.L3).convertToOWL(model, os);
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
                    String fieldName = parts[0]; //no trim() required here
                    String fieldValue = parts[1].trim();
                    if ("RecName: Full".equals(fieldName)) {
						proteinReference.setStandardName(fieldValue);
                    }
					else {
						proteinReference.addName(fieldValue);
                    }
                    //TODO ? do we really want to add all names from Contains: and Includes: sections?
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
     * @param model
     */
    private void setOrganism(String organismName, String organismTaxId, 
    		ProteinReference proteinReference, Model model) {
        String parts[] = organismTaxId.replaceAll(";", "").split("=");
        String taxId = parts[1];
        parts = organismName.split("\\("); // - by first occurrence of '('
        String name = parts[0].trim();
		BioSource bioSource = getBioSource(taxId, name, model);
		proteinReference.setOrganism(bioSource);
    }

    /**
     * Sets some BioPAX comments 
     * ("INTERACTION" sections of UniProt CC fields, 
     * gene symbols, and copyright.)
	 *
	 * @param comments String
	 * @param geneSynonyms String
	 * @param proteinReference ProteinReference
     */
    private void setComments (String comments, ProteinReference proteinReference) 
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
	 * @param dbRefs String (concatenated 'DR' lines)
     * @param proteinReference
     * @param model
     */
    private void setXRefsFromDRs (String dbRefs, ProteinReference proteinReference, Model model) {
        //split the line into like: "DB; ID1; ID2;" lines (also removing ending ".\n" or "-.\n")
    	final String xrefList[] = dbRefs.split("-?\\.\\s*\n"); 
        
        for (int i=0; i<xrefList.length; i++) {
        	String xref = xrefList[i].trim();
        	String parts[] = xref.split(";");
        	String db = parts[0].trim().toUpperCase();
    		
        	//adding for id in part[1], part[2],..
			if (db.equals("GENEID") || db.equals("REFSEQ") 
					|| db.equals("ENSEMBL") || db.equals("HGNC")) {
				if (db.equals("GENEID"))
					db = "NCBI GENE"; //- preferred name (synonym: Entrez Gene)
				for (int j = 1; j < parts.length; j++) {
			        String id = parts[j].trim();
			    	// extracting RefSeq AC from AC.Version identifier:
			    	if(db.equals("REFSEQ")) {
			    		int idx = id.lastIndexOf('.');
			    		if(idx > 0)
			    			id = id.substring(0, idx);
			    	} 
			    	else if (db.equals("ENSEMBL") && !id.startsWith("ENS"))
						continue;
					
			    	// add xref
					setRelationshipXRef(db, id, proteinReference, null, model);
				}
			}
        }
        	
    }

    
    /**
     * Gets Official Gene Symbols from UniProt record's GN fields.
     * 
     * @param geneName concatenated UniProt record's GN fields.
     * @param proteinReference
     */
    private Collection<String> getGeneSymbols(String geneName, ProteinReference proteinReference) 
    {
    	Collection<String> symbls = new ArrayList<String>();
        String parts[] = geneName.split(";\\s*(and)?");
        for (int i=0; i<parts.length; i++) {
            String subParts[] = parts[i].split("=");
            // can be >1 due to isoforms
            if (subParts[0].trim().equals("Name")) {
            	symbls.add(subParts[1]);
            }
        }
        return symbls;
    }

    /**
     * Gets Gene Synonyms from UniProt record's GN fields.
     * 
     * @param geneName concatenated UniProt record's GN fields 
     * @param proteinReference
     */
    private Collection<String> getGeneSynonyms(String geneName, ProteinReference proteinReference) 
    {
    	Collection<String> syns = new ArrayList<String>();
        String parts[] = geneName.split(";\\s*(and)?");
        for (int i=0; i<parts.length; i++) {
            String subParts[] = parts[i].split("=");
			if (subParts[0].trim().equals("Synonyms")) {
                String synList[] = subParts[1].split(",");
                for (int j=0; j<synList.length; j++) {
                    String currentSynonym = synList[j].trim();
                    syns.add(currentSynonym);
                }
            }
        }
        return syns;
    }
    
    
    /**
     * Sets Relationship XRefs.
	 *
     * @param dbName must be a MIRIAM preferred name for the data collection
     * @param id
     * @param proteinReference
     * @param relationshipType some term (e.g., "SEQUENCE", "GENE", "MRNA", etc.) or null.
     * @param model
     */
    private void setRelationshipXRef(String dbName, 
    	String id, ProteinReference proteinReference, String relationshipType, Model model) 
    {
        dbName = dbName.trim().toUpperCase();
    	//generate a URI using only the classname, (normalized) db name, and id (no rel. type)
		String uri = Normalizer.uri(model.getXmlBase(), dbName, id, RelationshipXref.class);
		RelationshipXref rXRef = (RelationshipXref) model.getByID(uri);
		if (rXRef == null) {
			rXRef = (RelationshipXref) model.addNew(RelationshipXref.class,	uri);
			rXRef.setDb(dbName);
			rXRef.setId(id);
		}
		
		//find/create a special relationship CV
		if (relationshipType != null) {
			String relCvId = Normalizer.uri(model.getXmlBase(), null,
					relationshipType.toUpperCase(), RelationshipTypeVocabulary.class);
			RelationshipTypeVocabulary relCv = (RelationshipTypeVocabulary) model.getByID(relCvId);
			if (relCv == null) {
				relCv = model.addNew(RelationshipTypeVocabulary.class, relCvId);
				relCv.addTerm(relationshipType.toUpperCase());
			}
			rXRef.setRelationshipType(relCv);
		}
		
		proteinReference.addXref(rXRef);
    }

    /**
     * Sets Unification XRefs.
	 * 
	 * @param dbName String
     * @param id tring
     * @param proteinReference ProteinReference
     * @param model
     */
    private void setUnificationXRef(String dbName, String id, ProteinReference proteinReference, Model model) {
        id = id.trim();
        dbName = dbName.trim().toUpperCase();
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
	 * Generates a new protein reference object (ProteinReference, BioPAX L3)
	 * from a pre-processed UniProt record: assigns the standard URI and 
	 * unification xrefs.
	 *
	 * @param shortName
	 * @param accessions AC field value - from the UniProt text format
	 * @param model 
	 * @return ProteinReference
	 */
	private ProteinReference newUniProtWithXrefs(String shortName, String accessions, Model model) 
	{	
		// accession numbers as array
		final String acList[] = accessions.split(";");
		// the first one, primary id, becomes the RDFId
		final String primaryId = acList[0].trim();
		final String uri = "http://identifiers.org/uniprot/" + primaryId;
		// create a new PR
		ProteinReference proteinReference = model.addNew(ProteinReference.class, uri);
		proteinReference.setDisplayName(shortName);
		
		// add all unification xrefs
		for (String acEntry : acList) {
			String ac = acEntry.trim();
			//use db='UNIPROT' for these xrefs instead of 'UniProt Knowledgebase' (preferred in MIRIAM)
			setUnificationXRef("UNIPROT", ac, proteinReference, model);
		}
		
		return proteinReference;
	}

	/**
	 * Gets a biosource
	 *
	 * @param taxId String
	 * @param name String
	 * @param model
	 * @return BioSource
	 */
	private BioSource getBioSource(String taxId, String name, Model model) 
	{
		// check taxonomy ID is integer value
		Integer taxonomy = null;
		try {
			taxonomy = Integer.valueOf(taxId);
		} catch (NumberFormatException e) {
			throw new RuntimeException("Faild to convert " + taxId
					+ " into integer taxonomy ID", e);
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
			final ProteinReference pr, Model model) 
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
				//TODO ? modificationFeature.setFeatureLocationType(regionVocabulary);
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
