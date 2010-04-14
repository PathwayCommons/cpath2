package cpath.converter.internal;

// imports
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
		log.info("convert(), creating Biopax Model.");
		createBPModel();
		log.info("convert(), model: " + bpModel);

        try {
			log.info("convert(), creating buffered reader.");
            reader = new InputStreamReader (is);
            BufferedReader bufferedReader = new BufferedReader(reader);
            String line = bufferedReader.readLine();
            HashMap dataElements = new HashMap();
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
                    BioPAXElement currentProteinOrER = getPhysicalEntity(shortName);
                    setNameAndSynonyms(currentProteinOrER, deField.toString());
                    setOrganism(organismName.toString(), organismTaxId.toString(), currentProteinOrER);
                    String geneSyns = null;
                    if (geneName != null) {
                        geneSyns= setGeneSymbolAndSynonyms(geneName.toString(), currentProteinOrER);
                    }
                    if (comments != null) {
                        setComments (comments.toString(), geneSyns, currentProteinOrER);
                    }
                    setUniProtAccessionNumbers(acNames.toString(), currentProteinOrER);
                    if (xrefs != null) {
                        setXRefs (xrefs.toString(), currentProteinOrER);
                    }
                    dataElements = new HashMap();
                }
				else {
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
        }
		catch(IOException e) {
			e.printStackTrace();
		}
		finally {
			log.info("convert(), closing reader.");
            if (reader != null) {
				try {
					reader.close();
				}
				catch (Exception e) {
					// ignore
				}
            }
        }
        log.info("convert(), exiting.");

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
        String name = null;
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
        //String rdfId = "BIO_SOURCE_NCBI_" + taxId;
        String rdfId = "urn:miriam:taxonomy:" + taxId;
		BioPAXElement bpSource = getBioSource(rdfId, taxId, name);
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
        HashSet <String> commentSet = new HashSet();
        commentSet.add(reducedComments.toString());
		if (bpLevel == BioPAXLevel.L2) {
			((Level2Element)currentProteinOrER).setCOMMENT(commentSet);
		}
		else if (bpLevel == BioPAXLevel.L3) {
			((Level3Element)currentProteinOrER).setComment(commentSet);
		}
    }

    /**
     * Sets UniProt Accession Numbers (can be 0, 1 or N).
     * However, we only take the 0th element, which is referred in UniProt as the
     * "Primary Accession Number".
     */
    private void setUniProtAccessionNumbers (String acNames, BioPAXElement currentProteinOrER) {
        String acList[] = acNames.split(";");
        if (acList.length > 0) {
			boolean createRDFId = true; // first id in list is primary
			for (String acEntry : acList) {
				String ac = acEntry.trim();
				setUnificationXRef("UNIPROT", ac, currentProteinOrER);
				if (createRDFId) {
					//currentProteinOrER.setRDFId("http://uniprot.org#urn%3Amiriam%3Auniprot%3A" + acEntry);
					currentProteinOrER.setRDFId("urn.miriam.uniprot:" + acEntry);
					createRDFId = false;
				}
			}
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
    private String setGeneSymbolAndSynonyms(String geneName, BioPAXElement currentProteinOrER) {
        StringBuffer synBuffer = new StringBuffer();
        String parts[] = geneName.split(";");
        for (int i=0; i<parts.length; i++) {
            String subParts[] = parts[i].split("=");
            // Set HUGO Gene Name
            if (subParts[0].trim().equals("Name")) {
                geneName = subParts[1];
                setRelationshipXRef("NGNC", geneName, currentProteinOrER);
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
        String rdfId = dbName + "_" +  id;
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
        String rdfId = dbName + "_" +  id;
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
	 * Gets a physical entity (or Entity Reference in L3)
	 *
	 * @param shortName String
	 * @return <T extends BioPAXElement>
	 */
	private <T extends BioPAXElement> T getPhysicalEntity(String shortName) {

		if (bpLevel == BioPAXLevel.L2) {
			physicalEntity toReturn = (physicalEntity)bpModel.addNew(protein.class, shortName);
			toReturn.setSHORT_NAME(shortName);
			return (T)toReturn;
		}
		else if (bpLevel == BioPAXLevel.L3) {
			SequenceEntityReference toReturn = (SequenceEntityReference)bpModel.addNew(ProteinReference.class, shortName + "_ER");
			toReturn.setDisplayName(shortName);
			return (T)toReturn;
		}

		// should not get here
		return null;
	}

	/**
	 * Gets a biosource
	 *
	 * @return <T extends BioPAXElement>
	 */
	private <T extends BioPAXElement> T getBioSource(String rdfId, String taxId, String name) {

		// check if biosource already exists
		if (bpModel.containsID(rdfId)) {
			return (T)bpModel.getByID(rdfId);
		}

		if (bpLevel == BioPAXLevel.L2) {
			bioSource toReturn = (bioSource)bpModel.addNew(bioSource.class, rdfId);
			toReturn.setNAME(name);
			unificationXref taxonXref = (unificationXref)bpModel.addNew(unificationXref.class, "TAXON_NCBI_" + taxId);
            taxonXref.setDB("NCBI_taxonomy");
            taxonXref.setID(taxId);
			toReturn.setTAXON_XREF(taxonXref);
			return (T)toReturn;
		}
		else if (bpLevel == BioPAXLevel.L3) {
			BioSource toReturn = (BioSource)bpModel.addNew(BioSource.class, rdfId);
			toReturn.setStandardName(name);
			UnificationXref taxonXref = (UnificationXref)bpModel.addNew(UnificationXref.class, "TAXON_NCBI_" + taxId);
			taxonXref.setDb("NCBI_taxonomy");
            taxonXref.setId(taxId);
			toReturn.setTaxonXref((UnificationXref)taxonXref);
			return (T)toReturn;
		}

		// should not get here
		return null;
	}

	private void createBPModel() {

		if (bpLevel == BioPAXLevel.L2) {
			bpModel = BioPAXLevel.L2.getDefaultFactory().createModel();
			bpModel.setFactory(new org.biopax.paxtools.proxy.level2.BioPAXFactoryForPersistence());
		}
		else if (bpLevel == BioPAXLevel.L3) {
			bpModel = BioPAXLevel.L3.getDefaultFactory().createModel();
			bpModel.setFactory(new org.biopax.paxtools.proxy.level3.BioPAXFactoryForPersistence());
		}

		// setup base
		//Map<String, String> nsMap = bpModel.getNameSpacePrefixMap();
		//nsMap.put("", "http://uniprot.org#");
	}
}
