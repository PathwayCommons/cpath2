package cpath.converter;

import cpath.service.CPathUtils;
import cpath.service.api.RelTypeVocab;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.BioSource;
import org.biopax.paxtools.model.level3.ModificationFeature;
import org.biopax.paxtools.model.level3.PositionStatusType;
import org.biopax.paxtools.model.level3.ProteinReference;
import org.biopax.paxtools.model.level3.RelationshipXref;
import org.biopax.paxtools.model.level3.SequenceModificationVocabulary;
import org.biopax.paxtools.model.level3.SequenceSite;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.normalizer.Normalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpath.service.api.Converter;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;

/**
 * Implementation of {@link Converter} interface for UniProt data.
 *
 * See also: http://web.expasy.org/docs/userman.html and http://www.uniprot.org/faq/30
 */
final class UniprotConverter extends BaseConverter {

    private static final Logger log = LoggerFactory.getLogger(UniprotConverter.class);

	public void convert(InputStream is, OutputStream os) {
		Model model = BioPAXLevel.L3.getDefaultFactory().createModel();
		model.setXmlBase(xmlBase);

		Scanner scanner = new Scanner(is, "UTF-8");
		final HashMap<String, StringBuilder> dataElements = new HashMap<>();
		log.info("convert(), starting to read data...");
		long linesReadSoFar = 0;
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			linesReadSoFar++;
			if (line.startsWith ("//")) //reached the end of a Uniprot entry
			{
				// grab properties from the map and prepare for parsing
				String deField = dataElements.get("DE").toString();
				String organismName = dataElements.get("OS").toString(); //mostly occurs once per entry
				String organismTaxId = dataElements.get("OX").toString(); //occurs once per entry
				StringBuilder comments = dataElements.get("CC");
				StringBuilder geneName = dataElements.get("GN");
				String acNames = dataElements.get("AC").toString();
				StringBuilder xrefs = dataElements.get("DR");
				final String idLine = dataElements.get("ID").toString();
// we will not process/save the canonical sequence -
//				StringBuilder sq = dataElements.get("SQ"); //SEQUENCE SUMMARY
//				StringBuilder sequence = dataElements.get("  "); //SEQUENCE
				StringBuilder features = dataElements.get("FT"); //strict format in 6-75 char in each FT line

				ProteinReference proteinReference = newProteinReferenceWithAccessionXrefs(idLine, acNames, model);

				// add some external xrefs from DR fileds
				if (xrefs != null) setXRefsFromDRs(xrefs.toString(), proteinReference, model);
				setNameAndSynonyms(proteinReference, deField);
				setOrganism(organismName, organismTaxId, proteinReference, model);

				// GN gene symbols - to PR names and rel. xrefs
				if (geneName != null) {
					Collection<String> geneNames = getGeneSymbols(geneName.toString(), proteinReference);
					// always use HGNC Symbol for rel. xrefs, despite it can be from MGI, RGD (these are coordinated by HGNC);
					// (cannot do this in setXRefsFromDRs: no gene synonyms there, and organism specific db names like MGI)
					for (String symbol : geneNames) {
						// also add Gene Names to PR names (can be >1 due to isoforms)
						proteinReference.addName(symbol);
						RelationshipXref rXRef = CPathUtils
								.findOrCreateRelationshipXref(RelTypeVocab.IDENTITY, "hgnc.symbol", symbol, model);
						proteinReference.addXref(rXRef);
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

// comments take too much space and can confuse full-text search (too generic, not specific for the model we build)
//				// add some info from CC fields to BioPAX comments
//				if (comments != null) {
//					setComments (comments.toString(), proteinReference);
//				}

// won't store canonical sequences (in practice, it does not help and may even mislead)

// won't generate MFs from FT,  MOD_RES records anymore (we don't use them; let's save storage/RAM for now)
//				//create modified residue features
//				if(features != null)
//					createModResFeatures(features.toString(), proteinReference, model);

				// debug: write the one-protein-reference model
				log.debug("convert(). so far line# " + linesReadSoFar);

				dataElements.clear();
			} else { //continue read and collect current Uniprot entry lines
				/* The two-character line-type code that begins each line is
					 * always followed by three blanks, so that the actual 
					 * information begins with the sixth character.
					 */
				String key = line.substring (0, 2);
				String data = line.substring(5);
				if (data.startsWith("-------") ||
						data.startsWith("Copyrighted") ||
						data.startsWith("Distributed")) {
					//  do nothing
				} else {
					//important for correct splitting DR rows
					if(key.equals("DR"))
						data += "\n";
					if (dataElements.containsKey(key)) {
						//remove leading spaces from second and next lines in FT, CC, DE records
						if(data.startsWith(" ")) //i.e, the sixth char on the line is space/blank
							data = data.replaceAll("^\\s+", "");
						dataElements.get(key).append(data);
					} else {
						dataElements.put(key, new StringBuilder (data));
					}
				}
			}
		}
		scanner.close();

        log.info("convert(), repairing.");
        model.repair();
        
        log.info("convert(), writing.");
        new SimpleIOHandler(BioPAXLevel.L3).convertToOWL(model, os);
    }


	/*
	 	* Sets name and synonyms on protein reference.
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
                    // after 1 Oct 2014, remove evidence (e.g., {type|source} - {ECO:0000269|PubMed10433554})
									  // at the name's end (see http://www.uniprot.org/changes/evidences)
                    int idx = fieldValue.indexOf(" {");
                    if(idx>0) fieldValue = fieldValue.substring(0, idx);
                    
                    if ("RecName: Full".equals(fieldName)) {
											proteinReference.setStandardName(fieldValue);
                    } else {
											proteinReference.addName(fieldValue);
                    }
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
     * @param model target biopax model
     */
		private void setOrganism(String organismName, String organismTaxId,
														 ProteinReference proteinReference, Model model) {
			String parts[] = organismTaxId.replaceAll(";", "").split("=");
			String taxId = parts[1];

			//since 1 Oct 2014, have to remove {evidence} after the taxId (after space); see http://www.uniprot.org/changes/evidences
			int idx = taxId.indexOf(" {");
			if(idx > 0) {
				taxId = taxId.substring(0, idx);
			}

			parts = organismName.split("\\("); // - by first occurrence of '('
			String name = parts[0].trim();
			BioSource bioSource = getBioSource(taxId, name, model);
			proteinReference.setOrganism(bioSource);
		}

    /**
     * Sets some BioPAX comments for the ProteinReference
     * (except for "INTERACTION" sections of UniProt CC fields)
		 *
		 * @param comments String
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
            reducedComments.append (" COPYRIGHT: UniProt Consortium (www.uniprot.org). Distributed under "
                    + "the Creative Commons Attribution-NoDerivs License.");
        }

			proteinReference.addComment(reducedComments.toString());
    }


    /*
     * Sets Multiple Types of XRefs, e.g. Entrez Gene ID and RefSeq.
	 *
	 * @param dbRefs String (concatenated 'DR' lines)
     */
    private void setXRefsFromDRs (String dbRefs, ProteinReference proteinReference, Model model) {
			final String lines[] = dbRefs.split("\n");

			for (String line : lines) {
				//remove everything after '.' (e.g., isoform refs, comments, etc.)
				String xref = line.trim();
				//every line ends with '.' or '. [blah-...]' something
				int lastDotIdx = xref.lastIndexOf(".");
				xref = xref.substring(0,lastDotIdx);
				String parts[] = xref.split(";");

				// get the db name part
				String db = parts[0].trim();

				// use only some of prot. ref. identity resources
				// (to make Xrefs and then use them for id-mapping);
				// skip for other, not identity, ID types,
				// e.g., refs to pathway databases, ontologies, etc.
				// see also: http://www.uniprot.org/docs/dbxref for the db name abbreviations used in 'DR' lines
				if ( // which 'DR' ID types do we want for id-mapping? (all are used by Merger; some - in queries)
						!db.equalsIgnoreCase("GENEID") // NCBI Gene (EntrezGene)
								&& !db.equalsIgnoreCase("REFSEQ")
								&& !db.equalsIgnoreCase("ENSEMBL")
								&& !db.equalsIgnoreCase("HGNC")
								&& !db.equalsIgnoreCase("PDB")
								&& !db.equalsIgnoreCase("IPI") //International Protein Index (deprecated; use UniProt)
								&& !db.equalsIgnoreCase("EMBL") //nucleotide sequence database
								&& !db.equalsIgnoreCase("PIR") //NCBI Protein
								&& !db.equalsIgnoreCase("PHARMGKB")
								&& !db.equalsIgnoreCase("DIP")
//					&& !db.equalsIgnoreCase("INTERPRO")
//					&& !db.equalsIgnoreCase("PANTHER") //PANTHER Family
// 					&& !db.equalsIgnoreCase("GENECARDS") //ID, not Symbol
//					&& !db.equalsIgnoreCase("DISPROT")
//					&& !db.equalsIgnoreCase("EGGNOG")
//					&& !db.equalsIgnoreCase("PRINTS")
//					&& !db.equalsIgnoreCase("PIRSF")
// 					&& !db.equalsIgnoreCase("PROSITE")
//					&& !db.equalsIgnoreCase("ORTHODB")
				) continue;

				String fixedDb = db;
				if (db.equalsIgnoreCase("GENEID"))
					fixedDb = "NCBI Gene"; // - preferred name

				//iterate over the ID tokens of the same DR line, skipping non-ID comments, etc. (ending)
				for (int j = 1; j < parts.length; j++) {
					String id = parts[j].trim();
					//at the end of a DR line in some cases (e.g, GeneID or RefSeq)?
					if(id.equals("-")) break;

					//skip PDB annotations
					if(db.equalsIgnoreCase("PDB") && !id.matches("^[0-9][A-Za-z0-9]{3}$")) break;
						//no more Ensembl IDs (skip comments)
					else if (db.equalsIgnoreCase("ENSEMBL") && !id.startsWith("ENS")) break;
						//no more InterPro IDs (skip comments)
//				else if (db.equalsIgnoreCase("INTERPRO") && !id.startsWith("IPR")) break;
//				else if (db.equalsIgnoreCase("PANTHER") && !id.startsWith("PTHR")) break;
//				else if (db.equalsIgnoreCase("PRINTS") && !id.startsWith("PR")) break;
					else if (db.equalsIgnoreCase("PHARMGKB") && !id.startsWith("PA")) break;
//				else if (db.equalsIgnoreCase("ORTHODB") && !id.startsWith("EOG")) break;
					else if (db.equalsIgnoreCase("DIP") && !id.startsWith("DIP-")) break;
					else if (db.equalsIgnoreCase("EMBL")) {
						if(!id.matches("^(\\w+\\d+(\\.\\d+)?)|(NP_\\d+)$"))
							break;
						fixedDb = "Nucleotide Sequence Database";
						//last ID in a HGNC line is in fact gene name
					} else if(db.equalsIgnoreCase("HGNC") && !id.startsWith("HGNC:")) {
						fixedDb = "hgnc.symbol";
					}
					//remove .version from RefSeq IDs
					else if (db.equalsIgnoreCase("REFSEQ")) {
						// extract only RefSeq AC from AC.Version ID form
						fixedDb = "refseq";
						id = id.replaceFirst("\\.\\d+", "");
					}

					//ok to create a new rel. xref with type "identity"
					RelationshipXref rXRef = CPathUtils.findOrCreateRelationshipXref(RelTypeVocab.IDENTITY, fixedDb, id, model);
					proteinReference.addXref(rXRef);
					// this xref type is then used for id-mapping in the Merger and queries;
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
			Collection<String> symbls = new ArrayList<>();
        String parts[] = geneName.split(";\\s*(and)?");
        for (int i=0; i<parts.length; i++) {
            String subParts[] = parts[i].split("=");
            // can be >1 due to isoforms
            if (subParts[0].trim().equals("Name")) {
							//remove {evidence}; see http://www.uniprot.org/changes/evidences (GN)
							String gn = subParts[1];
							int idx = gn.indexOf(" {");
							if(idx>0)
								gn = gn.substring(0, idx);
							symbls.add(gn);
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
			Collection<String> syns = new ArrayList<>();
        String parts[] = geneName.split(";\\s*(and)?");
        for (int i=0; i<parts.length; i++) {
            String subParts[] = parts[i].split("=");
					if (subParts[0].trim().equals("Synonyms")) {
                String synList[] = subParts[1].split(",");
                for (int j=0; j<synList.length; j++) {
                    String currentSynonym = synList[j].trim();
									//remove {evidence}; see http://www.uniprot.org/changes/evidences (GN)
									int idx = currentSynonym.indexOf(" {");
									if(idx>0)
										currentSynonym = currentSynonym.substring(0, idx);
                    syns.add(currentSynonym);
                }
            }
        }
        return syns;
    }


    /**
     * Sets Unification XRefs.
		 *  @param dbName value for 'db' property of the xref
     * @param id value for 'id' property of the xref
		 * @param proteinReference a protein reference to add the xref
		 * @param model the BioPAX model
		 */
    private void setUnificationXRef(String dbName, String id, ProteinReference proteinReference, Model model) {
      id = id.trim();
      dbName = dbName.trim();
			String rdfId = Normalizer.uri(model.getXmlBase(), dbName, id, UnificationXref.class);

			UnificationXref x = (UnificationXref) model.getByID(rdfId);
			if (x == null) {
				x = model.addNew(UnificationXref.class, rdfId);
				x.setDb(dbName);
				x.setId(id);
			}

			proteinReference.addXref(x);
    }

	/*
	 * Generates a new protein reference object (ProteinReference, BioPAX L3)
	 * from a pre-processed UniProt record: assigns the standard URI and 
	 * unification xrefs.
	 */
	private ProteinReference newProteinReferenceWithAccessionXrefs(String idLine, String accessions, Model model) {
		// accession numbers as array
		final List<String> acList = new ArrayList<>(Arrays.asList(accessions.split(";")));
		// Pop the first item, the primary AC, to generate canonical URI and unif. xref:
		final String primaryId = acList.remove(0).trim();
		final String uri = "http://bioregistry.io/uniprot:" + primaryId;

		// create a new PR with the name and primary unification xref
		ProteinReference proteinReference = model.addNew(ProteinReference.class, uri);
		String entryId = idLine.split("\\s+")[0]; //such as 'CALM_HUMAN'
		proteinReference.setDisplayName(entryId);
		//also use the ID (e.g., CALM_HUMAN) for a special RelationshipXref
		RelationshipXref rXRef = CPathUtils.findOrCreateRelationshipXref(RelTypeVocab.IDENTITY, "uniprot", entryId, model);
		proteinReference.addXref(rXRef);

		//add the primary accession number unification xref
		setUnificationXRef("uniprot", primaryId, proteinReference, model);

		// add 'secondary-ac' type RXs:
		for (String acEntry : acList) {
			rXRef = CPathUtils.findOrCreateRelationshipXref(
					RelTypeVocab.SECONDARY_ACCESSION_NUMBER, "uniprot", acEntry.trim(), model);
			proteinReference.addXref(rXRef);
		}
		proteinReference.addComment(idLine);

		return proteinReference;
	}

	/**
	 * Gets a biosource
	 */
	private BioSource getBioSource(String taxId, String name, Model model) {
		// check taxonomy ID is integer value
		Integer taxonomy;
		try {
			taxonomy = Integer.valueOf(taxId);
		} catch (NumberFormatException e) {
			throw new RuntimeException("Failed to convert " + taxId + " into integer taxonomy ID", e);
		}

		BioSource toReturn;
		// check the organism was previously used, re-use it
		if(taxonomy==null || taxonomy <= 0) {
			throw new RuntimeException("Illegal taxonomy ID: " + taxId);
		} else {
			String uri = "http://bioregistry.io/ncbitaxon:" + taxonomy;
			if (model.containsID(uri)) {
				toReturn = (BioSource) model.getByID(uri);
			} else {
				toReturn = model.addNew(BioSource.class, uri);
				toReturn.setStandardName(name);
				toReturn.setDisplayName(name);
				UnificationXref taxonXref = model.addNew(UnificationXref.class, Normalizer
						.uri(model.getXmlBase(), "ncbitaxon", taxId, UnificationXref.class));
				taxonXref.setDb("ncbi taxonomy");
				taxonXref.setId(taxId);
				toReturn.addXref(taxonXref);
			}
		}
		return toReturn;
	}


	/*
	 * Parses only "FT   MOD_RES   N    M   Term..." lines data and creates protein modification features and sites;
	 * original data line cannot exceed 70 chars, but can span multiple lines and ends with '.'
	 */
	private void createModResFeatures(final String features,
																		final ProteinReference pr, Model model) {
		// using a special "not greedy" regex!
		Pattern pattern = Pattern.compile("MOD_RES\\s+(\\d+)\\s*/note=\"([^\"]+)\"");
		Matcher matcher = pattern.matcher(features);
		int mfIndex = 0;
		while(matcher.find()) {
			// split the 2nd group by ';' (could be e.g. "Phosphothreonine; by CaMK4")
			// to extract the modification type and create the standard PSI-MOD synonym;
			String[] terms = matcher.group(2).split(";");
			String mod = terms[0];

			//(this seems for older format, but does not hurt to keep...)
			//remove non-standard ending comment from the standard CV term, things like
			//"...(By similarity).", or "...(Probable).", "...(Potential)." -
			//not greedy; i.e get "N6-(pyridoxal phosphate)lysine", not "N6-", from "N6-(pyridoxal phosphate)lysine (By similarity)."
			mod = mod.replaceFirst("\\([^()]+?\\)$","").trim();

			// Create the feature with CV and location -
			mfIndex++;
			String uri = Normalizer.uri(model.getXmlBase(),
					null, pr.getDisplayName() + "_" + mfIndex, ModificationFeature.class);
			ModificationFeature modificationFeature = model.addNew(ModificationFeature.class, uri);

			// get/create a new PSI-MOD SequenceModificationVocabulary (can be shared by many PRs)
			uri = Normalizer.uri(model.getXmlBase(), "mod", mod, SequenceModificationVocabulary.class);
			// so, let's check if it exists in the temp. or target model:
			SequenceModificationVocabulary cv = (SequenceModificationVocabulary) model.getByID(uri);
			if(cv == null) {
				cv = model.addNew(SequenceModificationVocabulary.class, uri);
				cv.addTerm(mod);
			}
			modificationFeature.setModificationType(cv);
			final int loc = Integer.parseInt(matcher.group(1));
			// so, let's check if the site exists in the temp. model
			String idPart = pr.getDisplayName() + "_" + loc;
			uri = Normalizer.uri(model.getXmlBase(), null, idPart, SequenceSite.class);
			SequenceSite ss = (SequenceSite) model.getByID(uri);
			if(ss == null) {
				ss = model.addNew(SequenceSite.class, uri);
				ss.setPositionStatus(PositionStatusType.EQUAL);
				ss.setSequencePosition(loc);
			}
			modificationFeature.setFeatureLocation(ss);
			pr.addEntityFeature(modificationFeature);
		}

	}
}
