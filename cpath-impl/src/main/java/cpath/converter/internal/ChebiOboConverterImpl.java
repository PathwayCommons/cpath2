package cpath.converter.internal;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.ChemicalStructure;
import org.biopax.paxtools.model.level3.PublicationXref;
import org.biopax.paxtools.model.level3.RelationshipXref;
import org.biopax.paxtools.model.level3.SmallMoleculeReference;
import org.biopax.paxtools.model.level3.StructureFormatType;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.validator.utils.Normalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpath.dao.CPathUtils;
import cpath.importer.PreMerger;
import cpath.importer.PreMerger.RelTypeVocab;


/**
 * Implementation of Converter interface for ChEBI OBO data.
 * This converter creates SMRs, xrefs, etc., and also 
 * sets memberEntityReference relationships  
 * among the compounds and classes.
 */
class ChebiOboConverterImpl extends BaseConverterImpl
{	
	private static Logger log = LoggerFactory.getLogger(ChebiOboConverterImpl.class);
	
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
	private final Pattern xrefPattern = Pattern.compile(".+?:(.+?)\\s+\"(.+?)\"");
	
	public void convert(InputStream is, OutputStream os) {		
        Model model = BioPAXLevel.L3.getDefaultFactory().createModel();
        model.setXmlBase(xmlBase); //important
        
        try {
        	//read&copy the input stream content to a tmp file 
        	//(this will uncompress if zip stream and make it re-usable)
        	File f = File.createTempFile("chebi", "obo");
        	f.deleteOnExit();
        	FileOutputStream fos = new FileOutputStream(f);
        	CPathUtils.copy(is, fos);
        	fos.close();
        	
        	//First pass.
        	//Read each [TERM] data to create SMR with xrefs;
        	//skipping terms w/o InChI (e.g., pharma categories, pills, etc. generics)
        	BufferedReader reader = new BufferedReader(new FileReader(f));						
			String line;
			while ((line = reader.readLine()) != null) {
				
				if (!line.startsWith(_ENTRY_START)) {
					log.debug("Skip: " + line);
					continue;
				}

				Map<String, String> chebiEntryMap = new HashMap<String, String>();
				
				while ((line = reader.readLine()) != null) {
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

			reader.close();
			
			//Second pass - to generate member relationships and rel. xrefs to other chebi classes
			ChebiOntologyAnalysis analysis = new ChebiOntologyAnalysis();
			analysis.setInputStream(new FileInputStream(f));
			analysis.execute(model);
			
        }
		catch (Exception e) {
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
	

	private void buildSmallMoleculeReference(Model model,
			Map<String, String> chebiEntryMap) {
		
		//skip entries w/o InChIKey (e.g., top-level classes, pill/pharma terms)
		if(chebiEntryMap.get(_SYNONYM) == null 
				|| !chebiEntryMap.get(_SYNONYM).contains("InChIKey=")) 
		{
			log.debug("Skipped " + chebiEntryMap.get(_ID) + " - no InChIKey");
			return;
		}		
		
		//create new SMR and URI
		String id = chebiEntryMap.get(_ID);
		SmallMoleculeReference smr = model
			.addNew(SmallMoleculeReference.class, _IDENTIFIERS_ORG+"chebi/"+id);
		String xuri = Normalizer.uri(xmlBase, "CHEBI", id, UnificationXref.class);
		UnificationXref x = model.addNew(UnificationXref.class, xuri);
		x.setId(id);
		x.setDb("ChEBI");
		smr.addXref(x);
		
		// set displayName
		smr.setDisplayName(chebiEntryMap.get(_NAME));

		//comment
		smr.addComment(chebiEntryMap.get(_DEF));
 
		//add unification xrefs using alt_id (if any present)
		if(chebiEntryMap.get(_ALT_ID) != null) {
			String[] alt = chebiEntryMap.get(_ALT_ID).split("\t");
			for(String altid : alt) {
				//unless there is an error in the ontology, when an alt_id belongs 
				//to different entries, this should not throw "already have such element" exception:
				xuri = Normalizer.uri(xmlBase, "CHEBI", altid, UnificationXref.class);
				x = model.addNew(UnificationXref.class, xuri);
				x.setId(altid);
				x.setDb("ChEBI");
				smr.addXref(x);
			} 
		}
		
		//use synonyms to create names, structure, formula, and InChIKey rel.xref
		String[] synonyms = chebiEntryMap.get(_SYNONYM).split("\t");
		for(String sy : synonyms) {
			Matcher matcher = namePattern.matcher(sy);
			if(!matcher.find())
				throw new IllegalStateException("Pattern failed to find a quoted text within: " + sy);		
			
			String name = matcher.group(1);
			if(sy.contains("IUPAC_NAME")) {
				smr.setStandardName(name);
			}
			else if(sy.contains("InChIKey=")) {
				String inchikey = name.substring(9);//exclude the prefix			
				xuri = Normalizer.uri(xmlBase, "InChIKey", inchikey, RelationshipXref.class);
				RelationshipXref rx = (RelationshipXref) model.getByID(xuri);
				if(rx == null) {
					rx = model.addNew(RelationshipXref.class, xuri);
					rx.setDb("InChIKey");
					rx.setId(inchikey);
				}
				smr.addXref(rx);				
			}
			else if(sy.contains("InChI=")) {
				String structureUri = Normalizer
					.uri(xmlBase, null, name, ChemicalStructure.class);
				ChemicalStructure structure = (ChemicalStructure) model.getByID(structureUri);
				if(structure == null) {
					structure = model.addNew(ChemicalStructure.class, structureUri);
					structure.setStructureFormat(StructureFormatType.InChI);
					structure.setStructureData(name); //contains "InChI=" prefix
				}
				smr.setStructure(structure);
			}
			else if(sy.contains("FORMULA")) {
				smr.setChemicalFormula(name);
			}
			else {
				smr.addName(name); //incl. for SMILES
			}
		}		
			
		// add xrefs (external)
		if(chebiEntryMap.get(_XREF) != null) {
			String[] xrefs = chebiEntryMap.get(_XREF).split("\t");
			for(String xs : xrefs) {
				Matcher matcher = xrefPattern.matcher(xs);
				if(!matcher.find())
					throw new IllegalStateException("Pattern failed " +
							"to match xref id and db within " + xs);
				//ID w/o the "source:" prefix
				id = matcher.group(1);
				//remove quotes around the db name
				String db = matcher.group(2).toUpperCase();
				
				// Skip all xrefs but: CAS, KEGG (C* and D*), DRUGBANK, WIKIPEDIA,
				// which can be used for id-mapping by Merger and graph queries.
				if(db.startsWith("CAS") || db.startsWith("KEGG")
					|| db.startsWith("WIKIPEDIA") || db.equals("DRUGBANK")) 
				{
					RelationshipXref rx = PreMerger
						.findOrCreateRelationshipXref(RelTypeVocab.MAPPED_IDENTITY, db, id, model);
					smr.addXref(rx);
				} else if(db.startsWith("PUBMED")) {
					//add PublicationXref
					String pxrefUri = _IDENTIFIERS_ORG + "pubmed/"+id; //the Normalizer would return the same
					PublicationXref pxref = (PublicationXref) model.getByID(pxrefUri);
					if(pxref == null) {
						pxref = model.addNew(PublicationXref.class, pxrefUri);
						pxref.setDb("PUBMED");
						pxref.setId(id);
					}
					smr.addXref(pxref);
				}
			}
		}
	}

	private String removePrefix(String prefix, String line) {
		return line.substring(prefix.length()).trim();
	}

}
