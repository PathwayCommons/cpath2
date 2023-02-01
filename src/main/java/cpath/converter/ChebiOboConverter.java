package cpath.converter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cpath.service.api.RelTypeVocab;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.ChemicalStructure;
import org.biopax.paxtools.model.level3.RelationshipXref;
import org.biopax.paxtools.model.level3.SmallMoleculeReference;
import org.biopax.paxtools.model.level3.StructureFormatType;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.normalizer.Normalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpath.service.CPathUtils;

/**
 * Implementation of Converter interface for ChEBI OBO data.
 * This converter creates SMRs, xrefs, etc., and also 
 * sets memberEntityReference relationships  
 * among the compounds and classes.
 */
class ChebiOboConverter extends BaseConverter
{
	private static Logger log = LoggerFactory.getLogger(ChebiOboConverter.class);

	private final String _IDENTIFIERS_ORG = "http://identifiers.org/";
	private final String _ENTRY_START = "[Term]";
	private final String _ID = "id: ";
	private final String _ALT_ID = "alt_id: ";
	private final String _XREF = "xref: ";
	private final String _NAME = "name: ";
	private final String _DEF = "def: ";
	private final String _SYNONYM = "synonym: ";

	//to extract a text value between quotation marks from 'def:' and 'synonym:' lines:
	private final Pattern namePattern = Pattern.compile("\"(.+?)\"");
	//to extract ID, DB values from 'xref:' lines:
	//(since ChEBI OBO format has been slightly changed in 2017, pattern was updated)
	private final Pattern xrefPattern = Pattern.compile("(.+?):(\\S+)");

	public void convert(InputStream is, OutputStream os) {
		Model model = BioPAXLevel.L3.getDefaultFactory().createModel();
		model.setXmlBase(xmlBase); //important

		try {
			//read&copy the input stream content to a tmp file
			//(this will allow to read it twice; see below)
			Path f = Files.createTempFile("chebi", "obo");
			CPathUtils.copy(is, Files.newOutputStream(f));

			//The first pass.
			//Read each [TERM] data to create SMR with xrefs;
			//Do NOT skip any terms, even those without InChIKey (e.g., pharma categories, pills, generics)
			Scanner scanner = new Scanner(Files.newInputStream(f), "UTF-8");
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (!line.startsWith(_ENTRY_START)) {
					log.debug("Skip: " + line);
					continue;
				}

				Map<String, String> chebiEntryMap = new HashMap<String, String>();

				while (scanner.hasNextLine()) {
					line = scanner.nextLine();
					if (line.isEmpty())
						break;
					// start of entry
					if (line.startsWith(_ID)) {
						chebiEntryMap.put(_ID, removePrefix(_ID, line));
					}
					else if (line.startsWith(_NAME)) {
						chebiEntryMap.put(_NAME, removePrefix(_NAME, line));
					}
					else if (line.startsWith(_DEF)) {
						Matcher matcher = namePattern.matcher(line);
						if(!matcher.find())
							throw new IllegalStateException("Pattern failed to match a quoted comment in: " + line);
						chebiEntryMap.put(_DEF, matcher.group(1));
					}
					else if (line.startsWith(_ALT_ID)) {
						updateMapEntry(chebiEntryMap, _ALT_ID, line);
					}
					else if (line.startsWith(_SYNONYM)) {
						updateMapEntry(chebiEntryMap, _SYNONYM, line);
					}
					else if (line.startsWith(_XREF)) {
						updateMapEntry(chebiEntryMap, _XREF, line);
					}
				}

				buildSmallMoleculeReference(model, chebiEntryMap);
			}

			// The second pass -
			// to generate member relationships and rel. xrefs to other chebi classes
			ChebiOntologyAnalysis analysis = new ChebiOntologyAnalysis();
			analysis.setInputStream(Files.newInputStream(f,StandardOpenOption.DELETE_ON_CLOSE));
			analysis.execute(model);
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to convert ChEBI OBO to BioPAX", e);
		}

		new SimpleIOHandler(BioPAXLevel.L3).convertToOWL(model, os);
	}


	private void updateMapEntry(Map<String, String> map, String key, String line) {
		StringBuilder sb = new StringBuilder();
		String val = map.get(key);
		if(val != null)
			sb.append(val).append('\t');
		sb.append(removePrefix(key, line));
		map.put(key, sb.toString());
	}


	//It now generates an SMR for every ChEBI entry, even those without InChIKey (top classes, pill/pharma terms)
	private void buildSmallMoleculeReference(Model model, Map<String, String> chebiEntryMap) {
		// create new URI, SMR, and primary xref:
		String id = chebiEntryMap.get(_ID);
		SmallMoleculeReference smr = model
			.addNew(SmallMoleculeReference.class, _IDENTIFIERS_ORG+"chebi/"+id);
		String xuri = Normalizer.uri(xmlBase, "ChEBI", id, UnificationXref.class);
		UnificationXref x = model.addNew(UnificationXref.class, xuri);
		x.setId(id);
		x.setDb("ChEBI");
		smr.addXref(x);

		// set displayName
		smr.setDisplayName(chebiEntryMap.get(_NAME));
		//comment
		smr.addComment(chebiEntryMap.get(_DEF));

		//add rel. xrefs using alt_id (if any present)
		if(chebiEntryMap.get(_ALT_ID) != null) {
			String[] alt = chebiEntryMap.get(_ALT_ID).split("\t");
			for(String altid : alt) {
				RelationshipXref rx = CPathUtils
					.findOrCreateRelationshipXref(RelTypeVocab.SECONDARY_ACCESSION_NUMBER, "ChEBI", altid, model, false);
				smr.addXref(rx);
			}
		}

		//use synonyms to create names, structure, formula, and InChIKey rel.xref, if the field is present
		//i.e., if it's not a generic/class type chebi entry
		final String entry = chebiEntryMap.get(_SYNONYM);
		if(entry != null && !entry.isEmpty()) {
			String[] synonyms = entry.split("\t");
			for (String sy : synonyms) {
				Matcher matcher = namePattern.matcher(sy);
				if (!matcher.find())
					throw new IllegalStateException("Pattern failed to find a quoted text within: " + sy);

				String name = matcher.group(1); //get the name/value only

				if (sy.contains("IUPAC_NAME")) {
					smr.setStandardName(name);
				} else if (sy.contains("InChIKey")) {
					if (name.startsWith("InChIKey=")) {
						//exclude the prefix
						name = name.substring(9);
					}
					//add RX because a InChIKey can map to several CHEBI IDs
					RelationshipXref rx = CPathUtils
						.findOrCreateRelationshipXref(RelTypeVocab.IDENTITY, "InChIKey", name, model, false);
					smr.addXref(rx);
				} else if (sy.contains("InChI=")) {
					String structureUri = Normalizer
						.uri(xmlBase, null, name, ChemicalStructure.class);
					ChemicalStructure structure = (ChemicalStructure) model.getByID(structureUri);
					if (structure == null) {
						structure = model.addNew(ChemicalStructure.class, structureUri);
						structure.setStructureFormat(StructureFormatType.InChI);
						structure.setStructureData(name); //contains "InChI=" prefix
					}
					smr.setStructure(structure);
				} else if (sy.contains("FORMULA")) {
					smr.setChemicalFormula(name);
					smr.addName(name); //helps to map/search by name
				} else if (sy.contains("MASS")) {
					smr.setMolecularWeight(Float.parseFloat(name));
				} else if (sy.contains("CHARGE") || sy.contains("MONOISOTOPIC_MASS")) {
					//TODO: save as comments?..
				} else {
					smr.addName(name); //incl. for SMILES
				}
			}
		}

		// add xrefs (external)
		if(chebiEntryMap.get(_XREF) != null) {
			String[] xrefs = chebiEntryMap.get(_XREF).split("\t");
			for(String xs : xrefs) {
				Matcher matcher = xrefPattern.matcher(xs);
				if(matcher.find()) {
					String xid = matcher.group(2);
					String xdb = matcher.group(1);
					String DB = xdb.toUpperCase();
					// Skip all xrefs except CAS, KEGG (C*, D*), etc.,
					// which are used for id-mapping, merging, full-text search, and graph queries.
					if (DB.equals("CAS") || DB.equals("DRUGBANK") || DB.equals("HMDB")) {
						RelationshipXref rx = CPathUtils.findOrCreateRelationshipXref(RelTypeVocab.IDENTITY, xdb, xid, model, false);
						smr.addXref(rx);
					} else if (DB.startsWith("WIKIPEDIA")) {
						smr.addName(id);
					} else if (DB.equals("KEGG")) {
						if(xid.startsWith("C"))
							xdb += " Compound";
						else if(xid.startsWith("D"))
							xdb += " Drug";
						RelationshipXref rx = CPathUtils.findOrCreateRelationshipXref(RelTypeVocab.IDENTITY, xdb, xid, model, false);
						smr.addXref(rx);
					}
				} else {
					throw new IllegalStateException("Pattern failed " +
						"to match xref id and db within " + xs);
				}
			}
		}
	}

	private String removePrefix(String prefix, String line) {
		return line.substring(prefix.length()).trim();
	}

}
