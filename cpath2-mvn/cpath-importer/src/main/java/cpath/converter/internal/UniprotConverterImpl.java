package cpath.converter.internal;

import cpath.converter.Converter;

import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level2.*;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.BioPAXElement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.io.*;
import java.net.URLEncoder;

/**
 * Implementation of Converter interface for Uniprot data.
 */
public class UniprotConverterImpl implements Converter {

	// logger
    private static Log log = LogFactory.getLog(UniprotConverterImpl.class);

	// ref to bp model
	private Model bpModel;

	// ref to bp level
	private BioPAXLevel bpLevel;
	
	
	public static final String MODEL_NAMESPACE_PREFIX = "http://uniprot.org#";
	

	/**
	 * (non-Javadoc>
	 * @see cpath.converter.Converter#convert(java.io.InputStream, org.biopax.paxtools.model.BioPXLevel)
	 */
	public Model convert(final InputStream is, BioPAXLevel level) {

		// init args
		this.bpLevel = level;

		// ref to reader here so
		// we can close in finally clause
        InputStreamReader reader= null;

		// create a model
        if(log.isInfoEnabled())
        	log.info("convert(), creating Biopax Model.");
        
		createBPModel();
		
		if(log.isInfoEnabled())
			log.info("convert(), model: " + bpModel);

        try {
        	if(log.isInfoEnabled())
        		log.info("convert(), creating buffered reader.");
            reader = new InputStreamReader (is);
            BufferedReader bufferedReader = new BufferedReader(reader);
            String line = bufferedReader.readLine();
            HashMap<String, StringBuffer> dataElements = new HashMap<String, StringBuffer>();
            if(log.isInfoEnabled())
            	log.info("convert(), starting to read data...");
            while (line != null) {
                if (line.startsWith ("//")) {
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
                    
                    BioPAXElement currentProteinOrER = newUniProtWithXrefs(shortName, acNames, xrefs);
                    
                    setNameAndSynonyms(currentProteinOrER, deField.toString());
                    
                    setOrganism(organismName.toString(), organismTaxId.toString(), currentProteinOrER);
                    
                    String geneSyns = null;
                    if (geneName != null) {
                        geneSyns= setGeneSymbolAndSynonyms(geneName, currentProteinOrER);
                    }
                    
                    if (comments != null) {
                        setComments (comments.toString(), geneSyns, currentProteinOrER);
                    }
                    
                    dataElements = new HashMap<String, StringBuffer>();
                } else {
                    String key = line.substring (0, 2);
                    String data = line.substring(5);
                    if (data.startsWith("-------") ||
                            data.startsWith("Copyrighted") ||
                            data.startsWith("Distributed")) {
                        //  do nothing here...
                    } else {
                        if (dataElements.containsKey(key)) {
                            StringBuffer existingData = (StringBuffer) dataElements.get(key);
                            existingData.append (" " + data);
                        } else {
                            dataElements.put(key, new StringBuffer (data));
                        }
                    }
                }
                line = bufferedReader.readLine();
            }
            if(log.isInfoEnabled()) 
            	log.info("convert(), no. of elements created: " 
            			+ bpModel.getObjects().size());
        }
		catch(IOException e) {
			log.error("Failed", e);
		}
		finally {
			if(log.isInfoEnabled()) log.info("convert(), closing reader.");
            if (reader != null) {
				try {
					reader.close();
				}
				catch (Exception e) {
					// ignore
				}
            }
        }
		if(log.isInfoEnabled()) log.info("convert(), exiting.");

		// outta here
		return bpModel;
    }

    private void setNameAndSynonyms (BioPAXElement currentProteinOrER, String deField) {
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
						if (bpLevel == BioPAXLevel.L2) {
							((physicalEntity)currentProteinOrER).setNAME(fieldValue);
						}
						else if (bpLevel == BioPAXLevel.L3) {
							((EntityReference)currentProteinOrER).setStandardName(fieldValue);
						}
                    } else {
						if (bpLevel == BioPAXLevel.L2) {
							((physicalEntity)currentProteinOrER).addSYNONYMS(fieldValue);
						}
						else if (bpLevel == BioPAXLevel.L3) {
							((EntityReference)currentProteinOrER).addName(fieldValue);
						}
                    }
                }
            }
        }
    }

    /**
     * Sets the Current Organism Information.
     */
    private void setOrganism(String organismName, String organismTaxId, BioPAXElement currentProteinOrER) {
        organismTaxId = organismTaxId.replaceAll(";", "");
        String parts[] = organismTaxId.split("=");
        String taxId = parts[1];
        parts = organismName.split("\\(");
        String name = parts[0].trim();
		BioPAXElement bpSource = getBioSource("urn:miriam:taxonomy:" + taxId, taxId, name);
		if (bpLevel == BioPAXLevel.L2) {
			((protein)currentProteinOrER).setORGANISM((bioSource)bpSource);
		}
		else if (bpLevel == BioPAXLevel.L3) {
			SequenceEntityReference ser = (SequenceEntityReference)currentProteinOrER;
			ser.setOrganism((BioSource)bpSource);
		}
    }

    /**
     * Sets Multiple Comments.
     */
    private void setComments (String comments, String geneSynonyms, BioPAXElement currentProteinOrER) {
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
		if (bpLevel == BioPAXLevel.L2) {
			((Level2Element)currentProteinOrER).setCOMMENT(commentSet);
		}
		else if (bpLevel == BioPAXLevel.L3) {
			((Level3Element)currentProteinOrER).setComment(commentSet);
		}
    }


    /**
     * Sets Multiple Types of XRefs, e.g. Entrez Gene ID and RefSeq.
     */
    private void setXRefs (String acNames, BioPAXElement currentProteinOrER) {
        String xrefList[] = acNames.split("\\.");

        for (int i=0; i<xrefList.length; i++) {
            String xref = xrefList[i].trim();
            if (xref.startsWith("GeneID")) {
                xref = xref.replaceAll("; -.", "");
                String parts[] = xref.split(";");
                String entrezGeneId = parts[1];
                setRelationshipXRef("ENTREZ GENE", entrezGeneId, currentProteinOrER);
            } else if (xref.startsWith("RefSeq")) {
                xref = xref.replaceAll("; -.", "");
                String parts[] = xref.split(";");
                String refSeqId = parts[1];
                if (refSeqId.contains(".")) {
                    parts = refSeqId.split("\\.");
                    refSeqId = parts[0];
                }
                setRelationshipXRef("REFSEQ", refSeqId, currentProteinOrER);
            }
        }
    }

    
    /**
     * Sets the HUGO Gene Symbol and Synonyms.
     */
    private String setGeneSymbolAndSynonyms(StringBuffer geneName, BioPAXElement currentProteinOrER) {
        StringBuffer synBuffer = new StringBuffer();
        String parts[] = geneName.toString().split(";");
        for (int i=0; i<parts.length; i++) {
            String subParts[] = parts[i].split("=");
            // Set HUGO Gene Name
            if (subParts[0].trim().equals("Name")) {
                setRelationshipXRef("NGNC", subParts[1], currentProteinOrER);
            } else if (subParts[0].trim().equals("Synonyms")) {
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
     */
    private void setRelationshipXRef(String dbName, String id, BioPAXElement currentProteinOrER) {
        id = id.trim();
        String rdfId = MODEL_NAMESPACE_PREFIX + URLEncoder.encode(dbName + "_" +  id);
        if (bpModel.containsID(rdfId)) {
			if (bpLevel == BioPAXLevel.L2) {
				relationshipXref rXRef = (relationshipXref) bpModel.getByID(rdfId);
				((physicalEntity)currentProteinOrER).addXREF(rXRef);
			}
			else if (bpLevel == BioPAXLevel.L3) {
				RelationshipXref rXRef = (RelationshipXref) bpModel.getByID(rdfId);
				((EntityReference)currentProteinOrER).addXref(rXRef);
			}
        } else {
			if (bpLevel == BioPAXLevel.L2) {
				relationshipXref rXRef = (relationshipXref)bpModel.addNew(relationshipXref.class, rdfId);
				rXRef.setDB(dbName);
				rXRef.setID(id);
				((physicalEntity)currentProteinOrER).addXREF(rXRef);
			}
			else if (bpLevel == BioPAXLevel.L3) {
				RelationshipXref rXRef = (RelationshipXref)bpModel.addNew(RelationshipXref.class, rdfId);
				rXRef.setDb(dbName);
				rXRef.setId(id);
				((EntityReference)currentProteinOrER).addXref(rXRef);
			}
        }
    }

    /**
     * Sets Unification XRefs.
     */
    private void setUnificationXRef(String dbName, String id, BioPAXElement currentProteinOrER) {
        id = id.trim();
        String rdfId = MODEL_NAMESPACE_PREFIX + URLEncoder.encode(dbName + "_" +  id);
        if (bpModel.containsID(rdfId)) {
			if (bpLevel == BioPAXLevel.L2) {
				unificationXref rXRef = (unificationXref) bpModel.getByID(rdfId);
				((physicalEntity)currentProteinOrER).addXREF(rXRef);
			}
			else if (bpLevel == BioPAXLevel.L3) {
				UnificationXref rXRef = (UnificationXref) bpModel.getByID(rdfId);
				((EntityReference)currentProteinOrER).addXref(rXRef);
			}
        } else {
			if (bpLevel == BioPAXLevel.L2) {
				unificationXref rXRef = (unificationXref)bpModel.addNew(unificationXref.class, rdfId);
				rXRef.setDB(dbName);
				rXRef.setID(id);
				((physicalEntity)currentProteinOrER).addXREF(rXRef);
			}
			else if (bpLevel == BioPAXLevel.L3) {
				UnificationXref rXRef = (UnificationXref)bpModel.addNew(UnificationXref.class, rdfId);
				rXRef.setDb(dbName);
				rXRef.setId(id);
				((EntityReference)currentProteinOrER).addXref(rXRef);
			}
        }
    }

	/**
	 * Gets a protein (or ProteinReference in L3);
	 * set its RDFId and all the xrefs.
	 *
	 * @param shortName
	 * @param accessions AC field values
	 * @param dbRefs DR field values
	 * @return
	 */
	private BioPAXElement newUniProtWithXrefs(String shortName, StringBuffer accessions, StringBuffer dbRefs) {
		BioPAXElement element = null;
		
		// accession numbers as array
		String acList[] = accessions.toString().split(";");
		// the first one, primary id, becomes the RDFId
		String id = "urn.miriam.uniprot:" + acList[0].trim();
		
		if (bpLevel == BioPAXLevel.L2) {
			protein p = bpModel.addNew(protein.class, id);
			p.setSHORT_NAME(shortName);
			element = p;
		} else if (bpLevel == BioPAXLevel.L3) {
			ProteinReference p = bpModel.addNew(ProteinReference.class, id);
			p.setDisplayName(shortName);
			element = p;
		} else {
			throw new IllegalAccessError("Unsupported BioPAX Level : "
					+ bpLevel);
		}

		// add all unification xrefs
		for (String acEntry : acList) {
			String ac = acEntry.trim();
			setUnificationXRef("UNIPROT", ac, element);
		}
		
		// add other xrefs
        if (dbRefs != null) {
            setXRefs (dbRefs.toString(), element);
        }
		
		return element;
	}

	/**
	 * Gets a biosource
	 *
	 * @return 
	 */
	private BioPAXElement getBioSource(String rdfId, String taxId, String name) {

		// check if biosource already exists
		if (bpModel.containsID(rdfId)) {
			return bpModel.getByID(rdfId);
		}

		if (bpLevel == BioPAXLevel.L2) {
			bioSource toReturn = (bioSource)bpModel.addNew(bioSource.class, rdfId);
			toReturn.setNAME(name);
			unificationXref taxonXref = (unificationXref)bpModel
				.addNew(unificationXref.class, MODEL_NAMESPACE_PREFIX + "TAXONOMY_" + taxId);
            taxonXref.setDB("TAXONOMY");
            taxonXref.setID(taxId);
			toReturn.setTAXON_XREF(taxonXref);
			return toReturn;
		}
		else if (bpLevel == BioPAXLevel.L3) {
			BioSource toReturn = (BioSource)bpModel.addNew(BioSource.class, rdfId);
			toReturn.setStandardName(name);
			UnificationXref taxonXref = (UnificationXref)bpModel
				.addNew(UnificationXref.class, MODEL_NAMESPACE_PREFIX + "TAXONOMY_" + taxId);
			taxonXref.setDb("TAXONOMY");
            taxonXref.setId(taxId);
			toReturn.setTaxonXref((UnificationXref)taxonXref);
			return toReturn;
		}

		// should not get here
		return null;
	}

	
	private void createBPModel() {
		if (bpLevel == BioPAXLevel.L2) {
			//bpModel = BioPAXLevel.L2.getDefaultFactory().createModel();
			//bpModel.setFactory(new org.biopax.paxtools.proxy.level2.BioPAXFactoryForPersistence());
			bpModel = (new org.biopax.paxtools.proxy.level2.BioPAXFactoryForPersistence()).createModel();
		}
		else if (bpLevel == BioPAXLevel.L3) {
			//bpModel = BioPAXLevel.L3.getDefaultFactory().createModel();
			//bpModel.setFactory(new org.biopax.paxtools.proxy.level3.BioPAXFactoryForPersistence());
			bpModel = (new org.biopax.paxtools.proxy.level3.BioPAXFactoryForPersistence()).createModel();
		}

		// setup base
		Map<String, String> nsMap = bpModel.getNameSpacePrefixMap();
		nsMap.put("", MODEL_NAMESPACE_PREFIX);
	}
}
