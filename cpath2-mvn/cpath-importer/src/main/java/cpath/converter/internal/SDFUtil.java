package cpath.converter.internal;

// imports
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.level3.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.io.StringReader;
import java.io.IOException;
import java.io.BufferedReader;

/**
 * Class which contains shared methods
 * used by both PubChem and ChEBI SDF to biopax converters.
 */
public class SDFUtil {

    // SOURCE enum
    public static enum SOURCE {

        // command types
        PUBCHEM("PUBCHEM"),
		CHEBI("CHEBI");

        // string ref for readable name
        private String source;
        
        // contructor
        SOURCE(String source) { this.source = source; }

        // method to get enum readable name
        public String toString() { return source; }
    }

	// logger
    private static Log log = LogFactory.getLog(SDFUtil.class);

	// some statics
	private static final String CHEBI_ID = "> <ChEBI ID>";
	private static final String PUBCHEM_ID = "";

	private static final String CHEBI_NAME = "> <ChEBI Name>";
	private static final String PUBCHEM_NAME = "";

	private static final String CHEBI_DESC = "> <Definition>";
	private static final String PUBCHEM_DESC = "";

	private static final String CHEBI_SECONDARY_ID = "> <Secondary ChEBI ID>";
	private static final String PUBCHEM_SECONDARY_ID = "";

	private static final String CHEBI_SMILES = "> <SMILES>";
	private static final String PUBCHEM_SMILES = "";

	private static final String CHEBI_INCHI = "> <InChI>";
	private static final String PUBCHEM_INCHI = "";

	private static final String CHEBI_INCHI_KEY = "> <InChIKey>";
	private static final String PUBCHEM_INCHI_KEY = "";

	private static final String CHEBI_MASS = "> <Mass>";
	private static final String PUBCHEM_MASS = "";

	private static final String CHEBI_IUPAC_NAME = "> <IUPAC Names>";
	private static final String PUBMED_IUPAC_NAME = "";

	private static final String CHEBI_SYNONYMS = "> <Synonyms>";
	private static final String PUBMED_SYNONYMS = "";

	private static final String CHEBI_BEILSTEIN = "> <Beilstein Registry Numbers>";
	private static final String PUBCHEM_BEILSTEIN = "";

	private static final String CHEBI_CAS = "> <CAS Registry Numbers>";
	private static final String PUBCHEM_CAS = "";

	private static final String CHEBI_GMELIN = "> <Gmelin Registry Numbers>";
	private static final String PUBCHEM_GMELIN = "";

	private static final String CHEBI_DATABASE_REGEX = "> <(\\w+|\\w+\\-\\w+) Database Links>";
	private static final String PUBCHEM_DATABASE_REGEX = "";

	// ref to factory
	private BioPAXFactory factory;

	// source of SDF data
	private SOURCE source;

	// ref to model
	private Model bpModel;

	/**
	 * Constructor.
	 *
	 * @param source Source
	 * @param model Model
	 */
	public SDFUtil(SOURCE source, Model model) {
		this.source = source;
		this.bpModel = model;
		this.factory = BioPAXLevel.L3.getDefaultFactory();
	}

	/**
	 * Given an SDF entry, return a small molecule reference.
	 *
	 * @param stringBuffer StringBuffer
	 * @throws IOException
	 */
	public void setSmallMoleculeReference(StringBuffer entryBuffer) throws IOException {

		SmallMoleculeReference smallMoleculeReference = null;

		if (source == SOURCE.CHEBI) {
			String rdfID = getRDFID(entryBuffer);
			if (rdfID != null) {
				smallMoleculeReference = (SmallMoleculeReference)bpModel.addNew(SmallMoleculeReference.class, rdfID);
			}
			// name
			setDisplayName(entryBuffer, smallMoleculeReference);
			// comment
			setComment(entryBuffer, smallMoleculeReference);
			// secondary id
			setSecondaryIDs(entryBuffer, smallMoleculeReference);
			// smiles
			setSmiles(entryBuffer, smallMoleculeReference);
			// InChI
			setInChiProp(entryBuffer, CHEBI_INCHI, smallMoleculeReference);
			// InChIKey
			setInChiProp(entryBuffer, CHEBI_INCHI_KEY, smallMoleculeReference);
			// mass
			setMass(entryBuffer, smallMoleculeReference);
			// iupac
			setNames(entryBuffer, CHEBI_IUPAC_NAME, smallMoleculeReference);
			// synonyms
			setNames(entryBuffer, CHEBI_SYNONYMS, smallMoleculeReference);
			// registry numbers
			setRegistryNumbers(entryBuffer, CHEBI_BEILSTEIN, smallMoleculeReference);
			setRegistryNumbers(entryBuffer, CHEBI_CAS, smallMoleculeReference);
			setRegistryNumbers(entryBuffer, CHEBI_GMELIN, smallMoleculeReference);
			// database links
			setDatabaseLinks(entryBuffer, CHEBI_DATABASE_REGEX, smallMoleculeReference);
		}
		else if (source == SOURCE.PUBCHEM) {
			//TBD
		}
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
		BufferedReader reader = getBufferedReader(entry);
		
		if (source == SOURCE.CHEBI) {
			// next line contains id in form: CHEBI:15377
			String id = getValue(entry, CHEBI_ID);
			if (log.isInfoEnabled()) {
				log.info("getRDFID(), id: " + id);
			}
			if (id == null) return null;
			String[] parts = id.split(":");
			rdfID = "urn:miriam:" + parts[0].trim().toLowerCase() + ":" + parts[1].trim();
		}
		else if (source == SOURCE.PUBCHEM) {
			// TBD
			// String parts = getValue(entry, PUBCHEM_ID);
		}

		// outta here
		return rdfID;
	}

	/**
	 * Given an SDF entry,
	 * finds the display name and sets it on given ref.
	 *
	 * @param entry StringBuffer
	 * @param smallMoleculeReference SmallMoleculeReference
	 * @throws IOException
	 */
	private void setDisplayName(StringBuffer entry, SmallMoleculeReference smallMoleculeReference) throws IOException {

		String displayName = null;
		
		if (source == SOURCE.CHEBI) {
			displayName = getValue(entry, CHEBI_NAME);
		}
		else if (source == SOURCE.PUBCHEM) {
			// TBD
			//displayName = getValue(entry, PUBCHEM_NAME);
		}		

		// if we have one, set it
		if (displayName != null) {
			if (log.isInfoEnabled()) {
				log.info("setDisplayName(), displayName: " + displayName);
			}
			smallMoleculeReference.setDisplayName(displayName);
		}
	}

	/**
	 * Given an SDF entry, finds and sets the comment.
	 *
	 * @param stringBuffer StringBuffer
	 * @param smallMoleculeReference SmallMoleculeReference
	 * @throws IOException
	 */
	private void setComment(StringBuffer entry, SmallMoleculeReference smallMoleculeReference) throws IOException {

		String comment = null;
		
		if (source == SOURCE.CHEBI) {
			comment = getValue(entry, CHEBI_DESC);
		}
		else if (source == SOURCE.PUBCHEM) {
			// TBD
			//comment = getValue(entry, PUBCHEM_DESC);
		}		

		// if we have one, set it
		if (comment != null) {
			if (log.isInfoEnabled()) {
				log.info("setComment(), comment: " + comment);
			}
			smallMoleculeReference.addComment(comment);
		}
	}

	/**
	 * Given an SDF entry, finds and sets all secondary ids.
	 *
	 * @param entry StringBuffer
	 * @param smallMoleculeReference SmallMoleculeReference
	 * @throws IOException
	 */
	private void setSecondaryIDs(StringBuffer entry, SmallMoleculeReference smallMoleculeReference) throws IOException {

		Collection<String> secondaryIDs = null;
		
		if (source == SOURCE.CHEBI) {
			secondaryIDs = getValues(entry, CHEBI_SECONDARY_ID);
		}
		else if (source == SOURCE.PUBCHEM) {
			// TBD
			//secondaryIDs = getValues(entry, PUBCHEM_SECONDARY_ID);
		}

		// if we have ids, set them
		if (secondaryIDs != null) {
			if (log.isInfoEnabled()) {
				log.info("setSecondaryIDs(), secondaryID count: " + secondaryIDs.size());
			}
			for (String secondaryID : secondaryIDs) {
				if (log.isInfoEnabled()) {
					log.info("setSecondaryIDs(), secondaryID: " + secondaryID);
				}
				String[] parts = secondaryID.split(":");
				smallMoleculeReference.addXref(getUnificationXref(parts));
			}
		}
	}

	/**
	 * Given an SDF entry, finds and sets smiles.
	 *
	 * @param entry StringBuffer
	 * @param smallMoleculeReference SmallMoleculeReference
	 * @throws IOException
	 */
	private void setSmiles(StringBuffer entry, SmallMoleculeReference smallMoleculeReference) throws IOException {

		String smiles = null;
		
		if (source == SOURCE.CHEBI) {
			smiles = getValue(entry, CHEBI_SMILES);
		}
		else if (source == SOURCE.PUBCHEM) {
			// TBD
			//smiles = getValue(entry, PUBCHEM_SMILES);
		}		

		// if we have one, set it
		if (smiles != null) {
			if (log.isInfoEnabled()) {
				log.info("setSmiles(), smiles: " + smiles);
			}
			smallMoleculeReference.setChemicalFormula(smiles);
		}
	}

	/**
	 * Given an SDF entry, finds and sets InChI property
	 *
	 * @param entry StringBuffer
	 * @param inChiPropKey String
	 * @param smallMoleculeReference SmallMoleculeReference
	 * @throws IOException
	 */
	private void setInChiProp(StringBuffer entry, String inChiPropKey, SmallMoleculeReference smallMoleculeReference)
		throws IOException {

		String inChiProp = getValue(entry, inChiPropKey);

		// if we have one, set it
		if (inChiProp != null) {
			if (log.isInfoEnabled()) {
				log.info("setInChiProp(), prop key: " + inChiPropKey);
				log.info("setInChiProp(), prop: " + inChiProp);
			}
			String[] parts = inChiProp.split("=");
			smallMoleculeReference.addXref(getUnificationXref(parts));
		}
	}

	/**
	 * Given an SDF entry, finds and sets the mass.
	 *
	 * @param stringBuffer StringBuffer
	 * @param smallMoleculeReference SmallMoleculeReference
	 * @throws IOException
	 */
	private void setMass(StringBuffer entry, SmallMoleculeReference smallMoleculeReference) throws IOException {

		String mass = null;
		
		if (source == SOURCE.CHEBI) {
			mass = getValue(entry, CHEBI_MASS);
		}
		else if (source == SOURCE.PUBCHEM) {
			// TBD
			//mass = getValue(entry, PUBCHEM_MASS);
		}		

		// if we have one, set it
		if (mass != null) {
			try {
				if (log.isInfoEnabled()) {
					log.info("setMass(), mass: " + mass);
				}
				smallMoleculeReference.setMolecularWeight(Float.parseFloat(mass));
			}
			catch (NumberFormatException e) {
				// ignore
			}
		}
	}

	/**
	 * Given an SDF entry, finds and sets a named property (iupac, synonyms)
	 *
	 * @param entry StringBuffer
	 * @param namesPropKey String
	 * @param smallMoleculeReference SmallMoleculeReference
	 * @throws IOException
	 */
	private void setNames(StringBuffer entry, String namesPropKey, SmallMoleculeReference smallMoleculeReference)
		throws IOException {

		Collection<String> names = getValues(entry, namesPropKey);
		if (log.isInfoEnabled()) {
			log.info("setNames(), names prop key: " + namesPropKey);
			log.info("setNames(), names size: " + names.size());
		}

		for (String name : names) {
			if (log.isInfoEnabled()) {
				log.info("setNames(), name: " + name);
			}
			smallMoleculeReference.addName(name);
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
	private void setRegistryNumbers(StringBuffer entry, String registryPropKey, SmallMoleculeReference smallMoleculeReference)
		throws IOException {

		String registryName = getKeyName(registryPropKey);
		Collection<String> registryProps = getValues(entry, registryPropKey);

		if (log.isInfoEnabled()) {
			log.info("setRegistryNumbers(), registryPropKey: " + registryPropKey);
			log.info("setRegistryNumbers(), registryName: " + registryName);
			log.info("setRegistryNumbers(), registryProps size: " + registryProps.size());
		}

		for (String registryProp : registryProps) {
			if (log.isInfoEnabled()) {
				log.info("setRegistryNumbers(), registryProp: " + registryProp);
			}
			String[] parts = {registryName, registryProp};
			smallMoleculeReference.addXref(getUnificationXref(parts));
		}
	}

	/**
	 * Given an SDF entry, finds and sets all database links.
	 * All links are relationship xrefs with exception of pubchem.
	 *
	 * @param entry StringBuffer
	 * @param databaseRegex String
	 * @param smallMoleculeReference SmallMoleculeReference
	 * @throws IOException
	 */
	private void setDatabaseLinks(StringBuffer entry, String databaseRegex, SmallMoleculeReference smallMoleculeReference)
		throws IOException {

		if (log.isInfoEnabled()) {
			log.info("setDatabaseLinks(), databaseRegex: " + databaseRegex);
		}
		
		BufferedReader reader = getBufferedReader(entry);

		String line = reader.readLine();
		while (line != null) {
			if (line.matches(databaseRegex)) {
				String dbName = getKeyName(line);
				Collection<String> dbIDs = getValues(entry, line);
				if (log.isInfoEnabled()) {
					log.info("setDatabaseLinks(), dbIDs size: " + dbIDs.size());
				}
				for (String dbID : dbIDs) {
					if (log.isInfoEnabled()) {
						log.info("setDatabaseLinks(), dbID: " + dbID);
					}
					String[] parts = {dbName, dbID};
					if (dbName.toLowerCase().equals(SOURCE.PUBCHEM.toString())) {
						smallMoleculeReference.addXref(getUnificationXref(parts));
					}
					else {
						smallMoleculeReference.addXref(getRelationshipXref(parts));
					}
				}
			}
			line = reader.readLine();
		}
	}

	/**
	 * Given a string array containg a
	 * db name at pos 0 and db id at pos 1 returns a UnificationXref.
	 *
	 * @param parts String[]
	 * @return UnificationXref
	 */
	private UnificationXref getUnificationXref(String[] parts) {
		
		UnificationXref toReturn = null;

		String dbName = parts[0].trim().toLowerCase();
		String dbID = parts[1].trim();
		String rdfID = (BaseConverterImpl.L3_UNIFICATIONXREF_URI +
						URLEncoder.encode(dbName + "_" + dbID));

		if (log.isInfoEnabled()) {
			log.info("getUnificationXref(), rdfID: " + rdfID);
		}

		if (bpModel.containsID(rdfID)) {
			toReturn = (UnificationXref)bpModel.getByID(rdfID);
		}
		else {
			toReturn = (UnificationXref)bpModel.addNew(UnificationXref.class, rdfID);
			toReturn.setDb(dbName);
			toReturn.setId(dbID);
		}

		// outta here
		return toReturn;
	}

	/**
	 * Given a string array containg a
	 * db name at pos 0 and db id at pos 1 returns a RelationshipXref.
	 *
	 * @param parts String[]
	 * @return RelationshipXref
	 */
	private RelationshipXref getRelationshipXref(String[] parts) {
		
		RelationshipXref toReturn = null;

		String dbName = parts[0].trim().toLowerCase();
		String dbID = parts[1].trim();
		String rdfID = (BaseConverterImpl.L3_RELATIONSHIPXREF_URI +
						URLEncoder.encode(dbName + "_" + dbID));

		if (log.isInfoEnabled()) {
			log.info("getRelationshipXref(), rdfID: " + rdfID);
		}

		if (bpModel.containsID(rdfID)) {
			toReturn = (RelationshipXref)bpModel.getByID(rdfID);
		}
		else {
			toReturn = (RelationshipXref)bpModel.addNew(RelationshipXref.class, rdfID);
			toReturn.setDb(dbName);
			toReturn.setId(dbID);
			//toReturn.setRelationshipType(getRelationshipType(dbName));
		}

		// outta here
		return toReturn;
	}

	/**
	 * Given a database link db, returns proper relationship type.
	 *
	 * @param dbName String
	 * @return RelationshipTypeVocabulary
	 */
	private RelationshipTypeVocabulary getRelationshipType(String dbName) {

		RelationshipTypeVocabulary toReturn;

		// check if vocabulary already exists
		String id = ""; // convert dbName into some id

		if (log.isInfoEnabled()) {
			log.info("getRelationshipType(), id: " + id);
		}

		if (bpModel.containsID(id)) {
			toReturn = (RelationshipTypeVocabulary)bpModel.getByID(id);
		}
		else {
			// create a new cv
			toReturn =
				(RelationshipTypeVocabulary)bpModel.addNew(RelationshipTypeVocabulary.class, id);
			toReturn.addTerm(""); // convert dbName into some term
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

		if (log.isInfoEnabled()) {
			log.info("getValue(), key: " + key);
		}

		String line = reader.readLine();
		while (line != null) {
			if (line.startsWith(key)) {
				toReturn = reader.readLine();
				break;
			}
			line = reader.readLine();
		}

		if (log.isInfoEnabled()) {
			if (toReturn != null) {
				log.info("getValue(), returning: " + toReturn);
			}
			else {
				log.info("getValue(), value not found!");
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
	private Collection<String> getValues(StringBuffer entry, String key) throws IOException {

		Collection<String> toReturn = new ArrayList<String>();
		BufferedReader reader = getBufferedReader(entry);

		if (log.isInfoEnabled()) {
			log.info("getValues(), key: " + key);
		}

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

		if (log.isInfoEnabled()) {
			log.info("getValues, toReturn size: " + toReturn.size());
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

		return key.replace("^> <(\\w+) .*$", "$1");
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