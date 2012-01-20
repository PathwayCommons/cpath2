package cpath.converter.internal;

import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.controller.ModelUtils.RelationshipType;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.validator.utils.Normalizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.io.*;

/**
 * Implementation of Converter interface for Uniprot data.
 */
public class UniprotConverterImpl extends BaseConverterImpl {

	// logger
    private static Log log = LogFactory.getLog(UniprotConverterImpl.class);
    
    private final Map<Integer, BioSource> organisms;
    
    public UniprotConverterImpl() {
    	this(null);
    }
    
	public UniprotConverterImpl(Model model) {
		super(model);
		organisms = new HashMap<Integer, BioSource>();
	}

	/**
	 * (non-Javadoc>
	 * @see cpath.converter.Converter#convert(java.io.InputStream)
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
            HashMap<String, StringBuffer> dataElements = new HashMap<String, StringBuffer>();
            if (log.isInfoEnabled()) {
            	log.info("convert(), starting to read data...");
			}
            long linesReadSoFar = 0;
            while (line != null) {
            	linesReadSoFar++;
                if (line.startsWith ("//")) 
                {
					// create local model to add ER
					Model proteinReferenceModel = BioPAXLevel.L3.getDefaultFactory().createModel();

					// grab properties from map
                    StringBuffer deField = (StringBuffer) dataElements.get("DE");
                    StringBuffer id = (StringBuffer) dataElements.get("ID");
                    StringBuffer organismName = (StringBuffer) dataElements.get("OS");
                    StringBuffer organismTaxId = (StringBuffer) dataElements.get("OX");
                    StringBuffer comments = (StringBuffer) dataElements.get("CC");
                    StringBuffer geneName = (StringBuffer) dataElements.get("GN");
                    StringBuffer acNames = (StringBuffer) dataElements.get("AC");
                    StringBuffer xrefs = (StringBuffer) dataElements.get("DR");
                    String idParts[] = id.toString().split("\\s");
                    String shortName = idParts[0];
                    
                    ProteinReference proteinReference = newUniProtWithXrefs(proteinReferenceModel, shortName, acNames, xrefs);
                    
                    setNameAndSynonyms(proteinReference, deField.toString());
                    
                    setOrganism(proteinReferenceModel, organismName.toString(), organismTaxId.toString(), proteinReference);
                    
                    String geneSyns = null;
                    if (geneName != null) {
                        geneSyns= setGeneSymbolAndSynonyms(proteinReferenceModel, geneName, proteinReference);
                    }
                    
                    if (comments != null) {
                        setComments (comments.toString(), geneSyns, proteinReference);
                    }

                    // debug: write the one-protein-reference model
                    if(log.isDebugEnabled()) {
                    	//ByteArrayOutputStream out = new ByteArrayOutputStream();
                    	//(new SimpleIOHandler(BioPAXLevel.L3)).convertToOWL(proteinReferenceModel, out);
                    	log.debug("So far line# " + linesReadSoFar + 
                    		"; merging new protein reference (model):\n");// + out.toString());
                    }
                    
					// we have finished creating local model, merge into global one now
					model.merge(proteinReferenceModel);
                    
                    dataElements = new HashMap<String, StringBuffer>();
                }
				else {
                    String key = line.substring (0, 2);
                    String data = line.substring(5);
                    if (data.startsWith("-------") ||
                            data.startsWith("Copyrighted") ||
                            data.startsWith("Distributed")) {
                        //  do nothing here...
                    }
					else {
                        if (dataElements.containsKey(key)) {
                            StringBuffer existingData = (StringBuffer) dataElements.get(key);
                            existingData.append (" " + data);
                        }
						else {
                            dataElements.put(key, new StringBuffer (data));
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
        //  We only want DE:  RecName:Full
        if (deField != null && deField.length() > 0) {
            String deTemp = deField.toString();
            String fields[] = deTemp.split(";");
            for (String field: fields) {
                String parts[] = field.split("=");
                if (parts.length == 2) {
                    String fieldName = parts[0].trim();
                    String fieldValue = parts[1].trim();
                    if (fieldName.length() > 0 && fieldName.equals("RecName: Full")) {
						proteinReference.setStandardName(fieldValue);
                    }
					else {
						proteinReference.addName(fieldValue);
                    }
                }
            }
        }
    }

    /**
     * Sets the Current Organism Information.
	 *
	 * @param proteinReferenceModel Model
	 * @param organismName String
	 * @param organismTaxId String
	 * @param proteinReference ProteinReference
     */
    private void setOrganism(Model proteinReferenceModel, String organismName, String organismTaxId, ProteinReference proteinReference) {
        organismTaxId = organismTaxId.replaceAll(";", "");
        String parts[] = organismTaxId.split("=");
        String taxId = parts[1];
        parts = organismName.split("\\(");
        String name = parts[0].trim();
		BioSource bioSource = getBioSource(proteinReferenceModel, "urn:miriam:taxonomy:" + taxId, taxId, name);
		proteinReference.setOrganism(bioSource);
    }

    /**
     * Sets Multiple Comments.
	 *
	 * @param comments String
	 * @param geneSynonyms String
	 * @param proteinReference ProteinReference
     */
    private void setComments (String comments, String geneSynonyms, ProteinReference proteinReference) {

        String commentParts[] = comments.split("-!- ");
        StringBuffer reducedComments = new StringBuffer();
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
        HashSet<String> commentSet = new HashSet<String>();
        commentSet.add(reducedComments.toString());
		proteinReference.getComment().addAll(commentSet);
    }


    /**
     * Sets Multiple Types of XRefs, e.g. Entrez Gene ID and RefSeq.
	 *
	 * @param proteinReferenceModel Model
	 * @param acNames String
	 * @param proteinReference
     */
    private void setXRefs (Model proteinReferenceModel, String acNames, ProteinReference proteinReference) {
        String xrefList[] = acNames.split("\\.");

        for (int i=0; i<xrefList.length; i++) {
            String xref = xrefList[i].trim();
            if (xref.startsWith("GeneID")) {
                xref = xref.replaceAll("; -.", "");
                String parts[] = xref.split(";");
                String entrezGeneId = parts[1];
                setRelationshipXRef(proteinReferenceModel, "Entrez Gene", 
                		entrezGeneId, proteinReference, RelationshipType.GENE);
            }
			else if (xref.startsWith("RefSeq")) {
                xref = xref.replaceAll("; -.", "");
                String parts[] = xref.split(";");
                String refSeqId = parts[1];
                if (refSeqId.contains(".")) {
                    parts = refSeqId.split("\\.");
                    refSeqId = parts[0];
                }
                setRelationshipXRef(proteinReferenceModel, "RefSeq", 
                		refSeqId, proteinReference, RelationshipType.SEQUENCE);
            }
			else if (xref.startsWith("HGNC") 
					|| xref.startsWith("MGI") 
						|| xref.startsWith("RGD")) {
				//TODO this xref is created only for hs, mm, rn species; do we need this at all?..
                xref = xref.replaceAll("; -.", "");
                String parts[] = xref.split(";");
                String db = parts[0].trim();
                String id = parts[1];
                if (id.contains(".")) {
                    parts = id.split("\\.");
                    id = parts[0];
                }
                setRelationshipXRef(proteinReferenceModel, db, 
                		id, proteinReference, RelationshipType.GENE);
            }
        }
    }

    
    /**
     * Sets the HUGO Gene Symbol and Synonyms.
	 *
	 * @param proteinReference ProteinReference
     */
    private String setGeneSymbolAndSynonyms(Model proteinReferenceModel, StringBuffer geneName, ProteinReference proteinReference) {

        StringBuffer synBuffer = new StringBuffer();
        String parts[] = geneName.toString().split(";");
        for (int i=0; i<parts.length; i++) {
            String subParts[] = parts[i].split("=");
            // Set HUGO Gene Name
            if (subParts[0].trim().equals("Name")) {
//                setRelationshipXRef(proteinReferenceModel, "HGNC", subParts[1], proteinReference, RelationshipType.GENE);
            	// add the gene symbol to protein names
            	proteinReference.addName(subParts[1]);
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
	 * @param proteinReferenceModel Model
     * @param dbName String
     * @param id String
     * @param proteinReference ProteinReference
     * @param relationshipType names from {@link RelationshipType} enum.
     */
    private void setRelationshipXRef(Model proteinReferenceModel, String dbName, 
    	String id, ProteinReference proteinReference, RelationshipType relationshipType) 
    {
        id = id.trim();
		String rdfId = Normalizer.generateURIForXref(dbName, id, null, RelationshipXref.class);
		RelationshipXref rXRef = (RelationshipXref)proteinReferenceModel.addNew(RelationshipXref.class, rdfId);
		rXRef.setDb(dbName);
		rXRef.setId(id);
		
		//find/create a special relationship CV
		String relCvId = ModelUtils.relationshipTypeVocabularyUri(relationshipType.name());
		RelationshipTypeVocabulary relCv = (RelationshipTypeVocabulary) proteinReferenceModel.getByID(relCvId);
		if(relCv == null) {
			relCv = proteinReferenceModel.addNew(RelationshipTypeVocabulary.class, relCvId);
			relCv.addTerm(relationshipType.name());
		}
		rXRef.setRelationshipType(relCv);
		proteinReference.addXref(rXRef);
    }

    /**
     * Sets Unification XRefs.
	 * 
	 * @param proteinReferenceModel Model
	 * @param dbName String
	 * @param id tring
	 * @param proteinReference ProteinReference
     */
    private void setUnificationXRef(Model proteinReferenceModel, String dbName, String id, ProteinReference proteinReference) {

        id = id.trim();
		String rdfId = Normalizer.generateURIForXref(dbName, id, null, UnificationXref.class);
		UnificationXref rXRef = (UnificationXref)proteinReferenceModel.addNew(UnificationXref.class, rdfId);
		rXRef.setDb(dbName);
		rXRef.setId(id);
		proteinReference.addXref(rXRef);
    }

	/**
	 * Gets a protein (or ProteinReference in L3);
	 * set its RDFId and all the xrefs.
	 *
	 * @param proteinReferenceModel
	 * @param shortName
	 * @param accessions AC field values
	 * @param dbRefs DR field values
	 * @return ProteinReference
	 */
	private ProteinReference newUniProtWithXrefs(Model proteinReferenceModel, String shortName, StringBuffer accessions, StringBuffer dbRefs) {
		BioPAXElement element = null;
		
		// accession numbers as array
		String acList[] = accessions.toString().split(";");
		// the first one, primary id, becomes the RDFId
		String id = "urn:miriam:uniprot:" + acList[0].trim().toUpperCase();
		// create new pr
		ProteinReference proteinReference = proteinReferenceModel.addNew(ProteinReference.class, id);
		proteinReference.setDisplayName(shortName);

		// add all unification xrefs
		for (String acEntry : acList) {
			String ac = acEntry.trim();
			setUnificationXRef(proteinReferenceModel, "uniprot", ac, proteinReference);
		}
		
		// add other xrefs
        if (dbRefs != null) {
            setXRefs(proteinReferenceModel, dbRefs.toString(), proteinReference);
        }
		
		// outta here
		return proteinReference;
	}

	/**
	 * Gets a biosource
	 *
	 * @param proteinReferenceModel Model
	 * @param rdfId String
	 * @param taxId String
	 * @param name String
	 * @return BioSource
	 */
	private BioSource getBioSource(Model proteinReferenceModel, String rdfId,
			String taxId, String name) 
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
		} else if (organisms.containsKey(taxonomy)) {
			toReturn = organisms.get(taxonomy);
			proteinReferenceModel.add(toReturn);
			proteinReferenceModel.add(toReturn.getXref().iterator().next());
		} else {
			toReturn = (BioSource) proteinReferenceModel.addNew(
					BioSource.class, rdfId);
			toReturn.setStandardName(name);
			toReturn.setDisplayName(name);
			UnificationXref taxonXref = (UnificationXref) proteinReferenceModel
				.addNew(UnificationXref.class, 
					Normalizer.generateURIForXref("TAXONOMY", taxId, null, UnificationXref.class));
			taxonXref.setDb("taxonomy");
			taxonXref.setId(taxId);
			// TODO update when taxonXref is removed (deprecated property)
			toReturn.addXref((UnificationXref) taxonXref);
			// save to re-use when importing other proteins 
			organisms.put(taxonomy, toReturn);
		}
		return toReturn;
	}
}
