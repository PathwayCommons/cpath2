package cpath.converter.internal;

import java.io.*;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;

import cpath.config.CPathSettings;

/**
 * Implementation of Converter interface for ChEBI data.
 */
public class ChEBIConverterForTestingImpl extends BaseSDFConverterImpl
{
	// url to ChEBI OBO file
	private static final String CHEBI_OBO_FILE_URL = 
		"test_chebi_obo_data.dat";
	
	// logger
    private static Log log = LogFactory.getLog(ChEBIConverterImpl.class);

	//
	// chebi statics
	//
	private static final String CHEBI_ID = "> <ChEBI ID>";
	private static final String CHEBI_NAME = "> <ChEBI Name>";
	private static final String CHEBI_IUPAC_NAMES = "> <IUPAC Names>";
	private static final String CHEBI_SYNONYMS = "> <Synonyms>";
	private static final String CHEBI_DEFINITION = "> <Definition>";
	private static final String CHEBI_SECONDARY_ID = "> <Secondary ChEBI ID>";
	private static final String CHEBI_SMILES = "> <SMILES>";
	private static final String CHEBI_INCHI = "> <InChI>";
	private static final String CHEBI_INCHI_KEY = "> <InChIKey>";
	private static final String CHEBI_MASS = "> <Mass>";
	private static final String CHEBI_FORMULA = "> <Formulae>";
	private static final String CHEBI_BEILSTEIN = "> <Beilstein Registry Numbers>";
	private static final String CHEBI_CAS = "> <CAS Registry Numbers>";
	private static final String CHEBI_GMELIN = "> <Gmelin Registry Numbers>";
	private static final String CHEBI_DATABASE_REGEX = "> <(\\w+|\\w+\\-\\w+) Database Links>";
	private static final Pattern CHEBI_EXT_LINK_OR_REGISTRY_REGEX =
		Pattern.compile("> <(\\w+|\\w+\\-\\w+) (Registry Numbers|Database Links)>$");
	
	public ChEBIConverterForTestingImpl() {
		this(null);
	}
	
	public ChEBIConverterForTestingImpl(Model model) {
		super(model, CHEBI_OBO_FILE_URL);
		if (log.isDebugEnabled()) {
			log.debug("CHEBI OBO FILE URL: " + CHEBI_OBO_FILE_URL);
		}
	}

	/**
	 *  
	 *  
	 *
	 * @param stringBuffer StringBuffer
	 * @throws IOException
	 */
	@Override
	protected SmallMoleculeReference buildSmallMoleculeReference(StringBuffer entryBuffer) 
		throws IOException {
		
		SmallMoleculeReference toReturn = null;
		
        // grab rdf id, create SMR 
		String rdfID = getRDFID(entryBuffer);
		if (rdfID == null) {
			return null;
		}
		toReturn = factory.reflectivelyCreate(SmallMoleculeReference.class);
		toReturn.setRDFId(rdfID);
		// primary id (u.xref)
		toReturn.addXref(getXref(UnificationXref.class,getValue(entryBuffer, CHEBI_ID), "chebi"));
		// names
		setName(getValue(entryBuffer, CHEBI_NAME), DISPLAY_NAME, toReturn);
		setName(getValue(entryBuffer, CHEBI_NAME), STANDARD_NAME, toReturn);
		// comment
		setComment(getValue(entryBuffer, CHEBI_DEFINITION), toReturn);
		// secondary ids
		for (String id : getValues(entryBuffer, CHEBI_SECONDARY_ID)) {
			toReturn.addXref(getXref(UnificationXref.class, id, "chebi"));
		}		
		// chemical structure - we use InChI, not smiles
		String[] rdfIDParts = toReturn.getRDFId().split(COLON_DELIMITER);
		String chemicalStructureID = CPathSettings.CPATH_URI_PREFIX 
			+ "ChemicalStructure:" + rdfIDParts[2]+"_"+rdfIDParts[3];
		String structure = getValue(entryBuffer, CHEBI_INCHI);
		setChemicalStructure(structure, StructureFormatType.InChI, chemicalStructureID, toReturn);
		// inchi key unification xref
		String inchiKey = getValue(entryBuffer, CHEBI_INCHI_KEY);
		if (inchiKey != null && inchiKey.length() > 0) {
			String id = inchiKey.split(EQUALS_DELIMITER)[1];
			toReturn.addXref(getXref(UnificationXref.class, id, "InChI"));
		}
		else {
			if (log.isInfoEnabled()) {
				log.info("ChEBI entry without InChIKey : " + rdfID);
			}
		}
		// mass
		setMolecularWeight(getValue(entryBuffer, CHEBI_MASS), toReturn);
		// formula
		setChemicalFormula(getValue(entryBuffer, CHEBI_FORMULA), toReturn);
		// iupac names
		setName(getValues(entryBuffer, CHEBI_IUPAC_NAMES), toReturn);
		// synonyms
		setName(getValues(entryBuffer, CHEBI_SYNONYMS), toReturn);
		// registry numbers
		setChEBIRegistryNumbers(entryBuffer, CHEBI_BEILSTEIN, toReturn);
		setChEBIRegistryNumbers(entryBuffer, CHEBI_CAS, toReturn);
		setChEBIRegistryNumbers(entryBuffer, CHEBI_GMELIN, toReturn);
		// database links
		setChEBIDatabaseLinks(entryBuffer, CHEBI_DATABASE_REGEX, toReturn);
		
		// outta here
		return toReturn;
	}
	

	/**
	 * Given an SDF entry,
	 * finds and returns the id.
	 *
	 * @param entry StringBuffer
	 * @return String
	 * @throws IOException
	 */
	private String getRDFID(StringBuffer entry) throws IOException {
		
		String rdfID = null;
		
		// next line contains id in form: CHEBI:15377
		String id = getValue(entry, CHEBI_ID);
		if (id == null) return null;
		String[] parts = id.split(":");
		rdfID = "urn:miriam:chebi:" + parts[1].trim();

		if (log.isDebugEnabled()) {
			log.debug("getRDFID(), rdfID: " + rdfID);
		}

		// outta here
		return rdfID;
	}

	/**
	 * Adds the given names to the names set.
	 *
	 * @param names Collection<String>
	 * @param propertyName String
	 * @param smallMoleculeReference SmallMoleculeReference
	 */
	private void setName(Collection<String> names, SmallMoleculeReference smallMoleculeReference) {	
		
		for (String name : names) {
			setName(name, ADDITIONAL_NAME, smallMoleculeReference);
		}
	}

	/**
	 * Sets the name property with the given name.
	 *
	 * @param name String
	 * @param propertyName String
	 * @param smallMoleculeReference SmallMoleculeReference
	 */
	private void setName(String name, String propertyName, SmallMoleculeReference smallMoleculeReference) {

		if (name != null) {
			if (log.isDebugEnabled()) {
				log.debug("setName(), name: " + name);
				log.debug("setName(), property: " + propertyName);
			}
			if (propertyName.equals(DISPLAY_NAME)) {
				smallMoleculeReference.setDisplayName(name);
			}
			else if (propertyName.equals(STANDARD_NAME)) {
				smallMoleculeReference.setStandardName(name);
			}
			else if (propertyName.equals(ADDITIONAL_NAME)) {
				smallMoleculeReference.addName(name);
			}
		}
	}

	/**
	 * Adds the comment.
	 *
	 * @param comment String
	 * @param smallMoleculeReference SmallMoleculeReference
	 */
	private void setComment(String comment, SmallMoleculeReference smallMoleculeReference) {

		if (comment != null) {
			if (log.isDebugEnabled()) {
				log.debug("setComment(), comment: " + comment);
			}
			smallMoleculeReference.addComment(comment);
		}
	}

	/**
	 * Sets the chemical formula.
	 *
	 * @param formula String
	 * @param smallMoleculeReference SmallMoleculeReference
	 */
	private void setChemicalFormula(String formula, SmallMoleculeReference smallMoleculeReference) {

		if (formula != null) {
			if (log.isDebugEnabled()) {
				log.debug("setChemicalFormula(), formula: " + formula);
			}
			smallMoleculeReference.setChemicalFormula(formula);
		}
	}
	
	/**
	 * Sets the structure.
	 *
	 * @param structure String
	 * @param structureFormat TODO
	 * @param chemicalStructureID String
	 * @param smallMoleculeReference SmallMoleculeReference
	 */
	protected void setChemicalStructure(String structure, StructureFormatType structureFormat, 
			String chemicalStructureID, SmallMoleculeReference  smallMoleculeReference) {

		if (structure != null) {
			if (log.isDebugEnabled()) {
				log.debug("setStructure(), structure: " + structure);
			}
			// should only get one of these
			ChemicalStructure chemStruct = factory.reflectivelyCreate(ChemicalStructure.class);
			chemStruct.setRDFId(chemicalStructureID);
			chemStruct.setStructureData(URLEncoder.encode(structure));
			chemStruct.setStructureFormat(structureFormat);
			smallMoleculeReference.setStructure(chemStruct);
		}
	}

	/**
	 * Sets molecular weight.
	 *
	 * @param molecularWeight String
	 * @param smallMoleculeReference SmallMoleculeReference
	 */
	private void setMolecularWeight(String molecularWeight, SmallMoleculeReference smallMoleculeReference) {

		// if we have one, set it
		if (molecularWeight != null) {
			try {
				if (log.isDebugEnabled()) {
					log.debug("setMolecularWeight(), molecular weight: " + molecularWeight);
				}
				smallMoleculeReference.setMolecularWeight(Float.parseFloat(molecularWeight));
			}
			catch (NumberFormatException e) {
				log.error("setMolecularWeight(), skipping due to: " + e);
			}
		}
	}

	/**
	 * Given an SDF entry, finds and sets the given registry property
	 *
	 * @param entry StringBuffer
	 * @param registryPropKey String
	 * @param smallMoleculeReference SmallMoleculeReference
	 * @throws IOException
	 */
	private void setChEBIRegistryNumbers(StringBuffer entry, String registryPropKey, SmallMoleculeReference smallMoleculeReference) throws IOException {

		String registryName = getKeyName(registryPropKey);
		if (registryName != null) {
			Collection<String> registryProps = getValues(entry, registryPropKey);
			for (String registryProp : registryProps) {
				setRelationshipXref(registryProp, registryName, smallMoleculeReference);
			}
		}
	}

	/**
	 * Given an SDF entry, finds and sets all database links.
	 * All links are relationship xrefs with exception of pubchem (uxref).
	 *
	 * @param entry StringBuffer
	 * @param databaseRegex String
	 * @param smallMoleculeReference SmallMoleculeReference
	 * @throws IOException
	 */
	private void setChEBIDatabaseLinks(StringBuffer entry, String databaseRegex, 
			SmallMoleculeReference smallMoleculeReference) throws IOException {
		
		BufferedReader reader = getBufferedReader(entry);
		String line = reader.readLine();
		while (line != null) {
			if (line.matches(databaseRegex)) {
				String db = getKeyName(line);
				if (db != null) {
					Collection<String> ids = getValues(entry, line);
					for (String id : ids) {
						if (db.equalsIgnoreCase("pubchem")) {
							smallMoleculeReference.addXref(getXref(UnificationXref.class, id, db));	
						}
						else {
							setRelationshipXref(id, db, smallMoleculeReference);
						}
					}
				}
			}
			line = reader.readLine();
		}
	}

	/**
	 * Creates a relationship xref for the given id.
	 *
	 * @param id String
	 * @param db String
	 * @param smallMoleculeReference SmallMoleculeReference
	 */
	private void setRelationshipXref(String id, String db, SmallMoleculeReference smallMoleculeReference) {
		
		/*
		 * TODO do it in cleaner; here is ugly quick fix 
		 * It must be CHEBI:XXXXX (not just XXXXX), 
		 * but - not RHEA:XXXXX (just XXXXX  is right)
		 */
		if(id.toUpperCase().startsWith("RHEA:")) {
			id = id.replaceFirst("(?i)rhea:", "");
		} 
		
		if (log.isDebugEnabled()) {
			log.debug("setRelationshipXref(), id, db: " + id + ", " + db);
		}
		smallMoleculeReference.addXref(getXref(RelationshipXref.class, id, db));
	}

	/**
	 * Given an xref class and a string array containing a
	 * db name at pos 0 and db id at pos 1 returns a proper xref.
	 *
	 * @param parts
	 * @return <T extends Xref>
	 */
	protected <T extends Xref> T getXref(Class<T> aClass, String... parts) {	
		
		T toReturn = null;

		String id = parts[0].trim();
		String db = parts[1].trim();
		
		if (log.isDebugEnabled()) {
			log.debug("getXref(), id: " + id + ", db: " + db 
					+ ", type: " + aClass.getSimpleName());
		}
		
		String rdfID =  CPathSettings.CPATH_URI_PREFIX +
			aClass.getSimpleName() + ":" + 
			URLEncoder.encode(db.toUpperCase() + "_" + id);

		if (model.containsID(rdfID)) {
			toReturn = getById(rdfID, aClass);
		}
		else {
			toReturn = (T) factory.reflectivelyCreate(aClass);
			toReturn.setRDFId(rdfID);
			toReturn.setDb(db);
			toReturn.setId(id);
			
			// TODO doubt: should this apply for all ids?..
			if ("RelationshipXref".equals(aClass.getSimpleName())) {
				toReturn.setIdVersion("entry_name");
			}
			
			//Rem: SimpleMerger later finds and adds this xref to the model
		}

		// outta here
		return toReturn;
	}

	/**
	 * Given an SDF entry, searches for given key and returns 
	 * value associated with that key.
	 *
	 * @param entry StringBuffer
	 * @param key String
	 * @return String
	 * @throws IOException
	 */
	private String getValue(StringBuffer entry, String key) throws IOException {
		
		String toReturn = null;
		BufferedReader reader = getBufferedReader(entry);

		if (log.isDebugEnabled()) {
			log.debug("getValue(), key: " + key);
		}

		String line = reader.readLine();
		while (line != null) {
			if (line.startsWith(key)) {
				toReturn = reader.readLine();
				break;
			}
			line = reader.readLine();
		}

		if (log.isDebugEnabled()) {
			if (toReturn != null) {
				log.debug("getValue(), returning: " + toReturn);
			}
			else {
				log.debug("getValue(), value not found!");
			}
		}

		// outta here
		return toReturn;
	}

	/**
	 * Given an SDF entry, searches for given key and returns
	 * a collection of values associated with the key.
	 *
	 * @param entry StringBuffer
	 * @param key String
	 * @return Collection<String>
	 * @throws IOException
	 */
	private Collection<String> getValues(StringBuffer entry, String key) 
		throws IOException {

		Collection<String> toReturn = new ArrayList<String>();
		BufferedReader reader = getBufferedReader(entry);

		if (log.isDebugEnabled())
			log.debug("getValues(), key: " + key);

		String line = reader.readLine();
		while (line != null) {
			if (line.startsWith(key)) {
				// start reading all lines following
				// key until blank line is encountered
				String value = reader.readLine();
				while (value != null && value.length() > 0) {
					toReturn.add(value);
					value = reader.readLine();
				}
				break;
			}
			line = reader.readLine();
		}

		if (log.isDebugEnabled()) {
			log.debug("getValues, toReturn size: " + toReturn.size());
		}

		// outta here
		return toReturn;
	}
	
	/** 
	 * Given a registry or database key,
	 * returns the registry or database name.
	 *
	 * @param key
	 * @return String
	 */
	private String getKeyName(String key) {
		Matcher matcher = CHEBI_EXT_LINK_OR_REGISTRY_REGEX.matcher(key);
		return (matcher.find()) ? matcher.group(1) : null;
	}

	/**
	 * Given a string buffer representation of an entry,
	 * returns a buffered reader to the entry.
	 *
	 * @param entry StringBuffer
	 * @return BufferedReader
	 * @throws IOException
	 */
	private BufferedReader getBufferedReader(StringBuffer entry) throws IOException {
		return new BufferedReader (new StringReader(entry.toString()));
	}	
}