package cpath.converter.internal;

// imports
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
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

	// some statics to identify names methods
	private static final String DISPLAY_NAME = "DISPLAY_NAME";
	private static final String STANDARD_NAME = "STANDARD_NAME";
	private static final String ADDITIONAL_NAME = "ADDITIONAL_NAME";

	// some statics to identify secondary id delimiters
	private static final String COLON_DELIMITER = ":";
	private static final String EQUALS_DELIMITER = "=";

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
	private static final String CHEBI_DATABASE_REGEX = "> <(\\w+|\\w+-\\w+) Database Links>";

	// pubchem statics
	private static final Map<String, String> PUBCHEM_COMPOUND_ID_TYPE_MAP = new HashMap<String, String>();
	{
		PUBCHEM_COMPOUND_ID_TYPE_MAP.put("0", "Deposited Compound");
		PUBCHEM_COMPOUND_ID_TYPE_MAP.put("1", "Standardized Form of the Deposited Compound");
		PUBCHEM_COMPOUND_ID_TYPE_MAP.put("2", "Component of the Standardized Form");
		PUBCHEM_COMPOUND_ID_TYPE_MAP.put("3", "Neutralized Form of the Standardized Form");
		PUBCHEM_COMPOUND_ID_TYPE_MAP.put("4", "Deposited Mixture Component");
		PUBCHEM_COMPOUND_ID_TYPE_MAP.put("5", "Alternate Tautomer Form of the Standardized Form");
		PUBCHEM_COMPOUND_ID_TYPE_MAP.put("6", "Ionized pKa Form of the Standardized Form");
		PUBCHEM_COMPOUND_ID_TYPE_MAP.put("255", "Unspecified or Unknown Compound Type");
	}
	private static final String PUBCHEM_COMPOUND_ID = "> <PUBCHEM_COMPOUND_CID>";
	private static final String PUBCHEM_SUBSTANCE_ID = "> <PUBCHEM_SUBSTANCE_ID>";
	private static final String PUBCHEM_IUPAC_NAME = "> <PUBCHEM_IUPAC_NAME>";
	private static final String PUBCHEM_IUPAC_TRADITIONAL_NAME = "> <PUBCHEM_IUPAC_TRADITIONAL_NAME>";
	private static final String PUBCHEM_IUPAC_OPENEYE_NAME = "> <PUBCHEM_IUPAC_OPENEYE_NAME>";
	private static final String PUBCHEM_IUPAC_CAS_NAME = "> <PUBCHEM_IUPAC_CAS_NAME>";
	private static final String PUBCHEM_IUPAC_SYSTEMATIC_NAME = "> <PUBCHEM_IUPAC_SYSTEMATIC_NAME>";
	private static final String PUBCHEM_COMPOUND_ID_TYPE = "> <PUBCHEM_COMPOUND_ID_TYPE>";
	private static final String PUBCHEM_SUBSTANCE_COMMENT = "> <PUBCHEM_SUBSTANCE_COMMENT";
	private static final String PUBCHEM_MOLECULAR_FORMULA = "> <PUBCHEM_MOLECULAR_FORMULA>";
	private static final String PUBCHEM_OPENEYE_CAN_SMILES = "> <PUBCHEM_OPENEYE_CAN_SMILES>";
	private static final String PUBCHEM_PUBMED_ID = "> <PUBCHEM_PUBMED_ID>";
	private static final String PUBCHEM_GENBANK_NUCLEOTIDE_ID = "> <PUBCHEM_GENBANK_NUCLEOTIDE_ID>";
	private static final String PUBCHEM_GENBANK_PROTEIN_ID = "> <PUBCHEM_GENBANK_PROTEIN_ID>";
	private static final String PUBCHEM_NCBI_TAXONOMY_ID = "> <PUBCHEM_NCBI_TAXONOMY_ID>";
	private static final String PUBCHEM_NCBI_OMIM_ID = "> <PUBCHEM_NCBI_OMIM_ID>";
	private static final String PUBCHEM_NCBI_MMDB_ID = "> <PUBCHEM_NCBI_MMDB_ID>";
	private static final String PUBCHEM_NCBI_GENE_ID = "> <PUBCHEM_NCBI_GENE_ID>";
	private static final String PUBCHEM_NIST_INCHIKEY = "> <PUBCHEM_NIST_INCHIKEY>";
	private static final String PUBCHEM_NIST_INCHI = "> <PUBCHEM_NIST_INCHI>";
	private static final String PUBCHEM_EXT_DATASOURCE_NAME = "> <PUBCHEM_EXT_DATASOURCE_NAME>";
	private static final String PUBCHEM_EXT_SUBSTANCE_URL = "> <PUBCHEM_EXT_SUBSTANCE_URL>";
	private static final String PUBCHEM_EXT_SUBSTANCE_URL_REGEX = ".*regno|id|acc=(\\w+)\\W?.*";
	private static final String PUBCHEM_EXT_DATASOURCE_REGID = "> <PUBCHEM_EXT_DATASOURCE_REGID>";

	// xref db name statics
	private static final String PUBMED = "pubmed";
	private static final String GENBANK = "genbank";
	private static final String GENBANK_PROTEIN = "protein";
	private static final String GENBANK_NUCLEOTIDE = "nucleotide";
	private static final String NCBI_TAXONOMY = "ncbi_taxonomy";
	private static final String NCBI_OMIM = "ncbi_omim";
	private static final String NCBI_MMDB = "ncbi_mmdb";
	private static final String NCBI_GENE = "ncbi_gene";

	// source of SDF data
	private SOURCE source;

	// ref to model
	private Model model;

	/**
	 * Constructor.
	 *
	 * @param source Source
	 * @param model Model
	 */
	public SDFUtil(SOURCE source, Model model) {

		// init members
		this.source = source;
		this.model = model;
	}

	/**
	 * Given an SDF entry, return a small molecule reference.
	 *
	 * @param stringBuffer StringBuffer
	 * @throws IOException
	 */
	public void setSmallMoleculeReference(StringBuffer entryBuffer) throws IOException {

		// used later
		String inchi = "";
		String inchiKey = "";

        // rdf id / small molecule ref 
		String rdfID = getRDFID(entryBuffer);
		if (rdfID == null) {
			return;
		}

		// do not import any entity reference that does not have a smiles entry
		if (source == SOURCE.CHEBI) {
			String smiles = getValue(entryBuffer, CHEBI_SMILES);
			if (smiles == null || smiles.length() == 0) {
				log.info("ChEBI entry without smiles, id: " + rdfID);
				return;
			}
		}

		SmallMoleculeReference smallMoleculeReference =
			(SmallMoleculeReference)model.addNew(SmallMoleculeReference.class, rdfID);

		if (source == SOURCE.CHEBI) {
			setChEBISmallMoleculeReference(entryBuffer, smallMoleculeReference);
			inchiKey = getValue(entryBuffer, CHEBI_INCHI_KEY);
			inchi = getValue(entryBuffer, CHEBI_INCHI);
		}
		else if (source == SOURCE.PUBCHEM) {
			setPubChemSmallMoleculeReference(entryBuffer, smallMoleculeReference);
			inchiKey = getValue(entryBuffer, PUBCHEM_NIST_INCHIKEY);
			inchi = getValue(entryBuffer, PUBCHEM_NIST_INCHI);
		}

		// create "member entity reference" using inchi key
		if (inchiKey != null && inchiKey.length() > 0) {
			String[] rdfIDParts = rdfID.split(":");
			String inchiKeyParts[] = inchiKey.split(EQUALS_DELIMITER);
			String[] unificationXRefParts = { rdfIDParts[2], rdfIDParts[3] };
			String memberEntityReferenceID = "urn:inchi:" + inchiKeyParts[1];
			try {
				SmallMoleculeReference memberEntityRef =
					(SmallMoleculeReference)model.addNew(SmallMoleculeReference.class, memberEntityReferenceID);
				// create a unification xref to pubchem or chebi
				memberEntityRef.addXref(getXref(UnificationXref.class, unificationXRefParts));
				// create chem struct using inchi
				if (inchi != null) {
					String parts[] = inchi.split(EQUALS_DELIMITER);
					if (parts.length == 2) {
						String chemicalStructureID = memberEntityRef.getRDFId() + ":chemical_structure_1";
						setChemicalStructure(parts[1], chemicalStructureID, smallMoleculeReference);
					}
				}
			}
			catch (org.biopax.paxtools.util.IllegalBioPAXArgumentException e) {
				// ignore
				//System.out.println("Duplicate inchi/inchi key, rdfid: " + rdfID);
				//System.out.println("Duplicate inchi/inchi key, inchi: " + inchiKey);
			}
		}
	}

	/**
	 * Populates a small molecule reference using chebi props.
	 *
	 * @param entryBuffer StringBuffer
	 * @param smallMoleculeReference SmallMoleculeReference
	 * @throws IOException
	 */
	public void setChEBISmallMoleculeReference(StringBuffer entryBuffer, SmallMoleculeReference smallMoleculeReference) throws IOException {

		// names
		setName(getValue(entryBuffer, CHEBI_NAME), DISPLAY_NAME, smallMoleculeReference);
		setName(getValue(entryBuffer, CHEBI_NAME), STANDARD_NAME, smallMoleculeReference);
		// comment
		setComment(getValue(entryBuffer, CHEBI_DEFINITION), smallMoleculeReference);
		// secondary ids
		setUnificationXref(getValues(entryBuffer, CHEBI_SECONDARY_ID), COLON_DELIMITER, smallMoleculeReference);
		// smiles - chemical structure
		String chemicalStructureID = smallMoleculeReference.getRDFId() + ":chemical_structure_1";
		setChemicalStructure(getValue(entryBuffer, CHEBI_SMILES), chemicalStructureID, smallMoleculeReference);
		// mass
		setMolecularWeight(getValue(entryBuffer, CHEBI_MASS), smallMoleculeReference);
		// formula
		setChemicalFormula(getValue(entryBuffer, CHEBI_FORMULA), smallMoleculeReference);
		// iupac names
		setName(getValues(entryBuffer, CHEBI_IUPAC_NAMES), smallMoleculeReference);
		// synonyms
		setName(getValues(entryBuffer, CHEBI_SYNONYMS), smallMoleculeReference);
		// registry numbers
		setChEBIRegistryNumbers(entryBuffer, CHEBI_BEILSTEIN, smallMoleculeReference);
		setChEBIRegistryNumbers(entryBuffer, CHEBI_CAS, smallMoleculeReference);
		setChEBIRegistryNumbers(entryBuffer, CHEBI_GMELIN, smallMoleculeReference);
		// database links
		setChEBIDatabaseLinks(entryBuffer, CHEBI_DATABASE_REGEX, smallMoleculeReference);
	}

	/**
	 * Populates a small molecule reference using chebi props.
	 *
	 * @param entryBuffer StringBuffer
	 * @param smallMoleculeReference SmallMoleculeReference
	 * @throws IOException
	 */
	public void setPubChemSmallMoleculeReference(StringBuffer entryBuffer, SmallMoleculeReference smallMoleculeReference) throws IOException {

		// chemical formula
		setChemicalFormula(getValue(entryBuffer, PUBCHEM_MOLECULAR_FORMULA), smallMoleculeReference);
		// names
		setName(getValue(entryBuffer, PUBCHEM_IUPAC_NAME), DISPLAY_NAME, smallMoleculeReference);
		setName(getValue(entryBuffer, PUBCHEM_IUPAC_TRADITIONAL_NAME), STANDARD_NAME, smallMoleculeReference);
		setName(getValue(entryBuffer, PUBCHEM_IUPAC_OPENEYE_NAME), ADDITIONAL_NAME, smallMoleculeReference);
		setName(getValue(entryBuffer, PUBCHEM_IUPAC_CAS_NAME), ADDITIONAL_NAME, smallMoleculeReference);
		setName(getValue(entryBuffer, PUBCHEM_IUPAC_SYSTEMATIC_NAME), ADDITIONAL_NAME, smallMoleculeReference);
		// structure
		String chemicalStructureID = smallMoleculeReference.getRDFId() + ":chemical_structure_1";
		setChemicalStructure(getValue(entryBuffer, PUBCHEM_OPENEYE_CAN_SMILES), chemicalStructureID, smallMoleculeReference);
		// comment - compound id type
		String comment = getValue(entryBuffer, PUBCHEM_COMPOUND_ID_TYPE);
		comment = (comment != null) ? PUBCHEM_COMPOUND_ID_TYPE_MAP.get(comment) : comment;
		setComment(comment, smallMoleculeReference);
		// comment - substance id type
		comment = getValue(entryBuffer, PUBCHEM_SUBSTANCE_COMMENT);
		setComment(comment, smallMoleculeReference);
		// external datasource - use url as id
		String db = getValue(entryBuffer, PUBCHEM_EXT_DATASOURCE_NAME);
		if (db != null) {
			String id = getValue(entryBuffer, PUBCHEM_EXT_DATASOURCE_REGID);
			if (id != null) {
				// TODO use Miriam to extract ID from the URI
				// String standardId = getIdFromPubchemExtUrl(id);
				//if (id.matches(PUBCHEM_EXT_SUBSTANCE_URL_REGEX)) {
				String[] parts = { db, id};
				smallMoleculeReference.addXref(getXref(UnificationXref.class, parts));
				//}
			}
		}
		// pubmed
		for (String id : getValues(entryBuffer, PUBCHEM_PUBMED_ID)) {
			setPublicationXref(PUBMED + COLON_DELIMITER + id, COLON_DELIMITER, smallMoleculeReference);
		}
		// genbank - nucleotide
		for (String id : getValues(entryBuffer, PUBCHEM_GENBANK_NUCLEOTIDE_ID)) {
			String[] parts = { GENBANK, id };
			RelationshipXref xref = (RelationshipXref)getXref(RelationshipXref.class, parts);
			xref.setIdVersion(GENBANK_NUCLEOTIDE);
			smallMoleculeReference.addXref(xref);
		}
		// genbank - protein
		for (String id : getValues(entryBuffer, PUBCHEM_GENBANK_PROTEIN_ID)) {
			String[] parts = { GENBANK, id };
			RelationshipXref xref = (RelationshipXref)getXref(RelationshipXref.class, parts);
			xref.setIdVersion(GENBANK_PROTEIN);
			smallMoleculeReference.addXref(xref);
		}
		// taxid
		setPubChemDatabaseLinks(NCBI_TAXONOMY, getValues(entryBuffer, PUBCHEM_NCBI_TAXONOMY_ID), COLON_DELIMITER, smallMoleculeReference);
		// omim
		setPubChemDatabaseLinks(NCBI_OMIM, getValues(entryBuffer, PUBCHEM_NCBI_OMIM_ID), COLON_DELIMITER, smallMoleculeReference);
		// mmdb
		setPubChemDatabaseLinks(NCBI_MMDB, getValues(entryBuffer, PUBCHEM_NCBI_MMDB_ID), COLON_DELIMITER, smallMoleculeReference);
		// gene id
		setPubChemDatabaseLinks(NCBI_GENE, getValues(entryBuffer, PUBCHEM_NCBI_GENE_ID), COLON_DELIMITER, smallMoleculeReference);
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
			if (id == null) return null;
			String[] parts = id.split(":");
			rdfID = "urn:miriam:" + parts[0].trim().toLowerCase() + ":" + parts[1].trim();
		}
		else if (source == SOURCE.PUBCHEM) {
			String id = getValue(entry, PUBCHEM_COMPOUND_ID);
			id = (id == null) ? getValue(entry, PUBCHEM_SUBSTANCE_ID) : id;
			if (id == null) return null;
			rdfID = "urn:miriam:pubchem" + ":" + id;
		}

		if (log.isInfoEnabled()) {
			log.info("getRDFID(), rdfID: " + rdfID);
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
			if (log.isInfoEnabled()) {
				log.info("setName(), name: " + name);
				log.info("setName(), property: " + propertyName);
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
			if (log.isInfoEnabled()) {
				log.info("setComment(), comment: " + comment);
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
			if (log.isInfoEnabled()) {
				log.info("setChemicalFormula(), formula: " + formula);
			}
			smallMoleculeReference.setChemicalFormula(formula);
		}
	}

	/**
	 * Sets the structure.
	 *
	 * @param structure String
	 * @param chemicalStructureID String
	 * @param smallMoleculeReference SmallMoleculeReference
	 */
	private void setChemicalStructure(String structure, String chemicalStructureID, SmallMoleculeReference  smallMoleculeReference) {
		
		if (structure != null) {
			if (log.isInfoEnabled()) {
				log.info("setStructure(), structure: " + structure);
			}
			// should only get one of these
			ChemicalStructure chemStruct = (ChemicalStructure)model.addNew(ChemicalStructure.class, chemicalStructureID);
			chemStruct.setStructureData(structure);
			chemStruct.setStructureFormat(StructureFormatType.SMILES);
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
				if (log.isInfoEnabled()) {
					log.info("setMolecularWeight(), molecular weight: " + molecularWeight);
				}
				smallMoleculeReference.setMolecularWeight(Float.parseFloat(molecularWeight));
			}
			catch (NumberFormatException e) {
				log.info("setMolecularWeight(), molecular weight NumberFormatException, skipping.");
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
		Collection<String> registryProps = getValues(entry, registryPropKey);

		for (String registryProp : registryProps) {
			setUnificationXref(registryName + COLON_DELIMITER + registryProp,
							   COLON_DELIMITER, smallMoleculeReference);
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
	private void setChEBIDatabaseLinks(StringBuffer entry, String databaseRegex, SmallMoleculeReference smallMoleculeReference)
		throws IOException {

		BufferedReader reader = getBufferedReader(entry);
		String line = reader.readLine();
		while (line != null) {
			if (line.matches(databaseRegex)) {
				String dbName = getKeyName(line);
				Collection<String> dbIDs = getValues(entry, line);
				for (String dbID : dbIDs) {
					// use equals delimiter to avoid problems with such ids as: RHEA:10960
					String id = dbName + EQUALS_DELIMITER + dbID;
					if (dbName.toLowerCase().equals(SOURCE.PUBCHEM.toString())) {
						setUnificationXref(id, EQUALS_DELIMITER, smallMoleculeReference);
					}
					else {
						setRelationshipXref(id, EQUALS_DELIMITER, smallMoleculeReference);
					}
				}
			}
			line = reader.readLine();
		}
	}

	/**
	 * Sets pubchem database links (relationship xrefs).
	 *
	 * @param db String
	 * @param ids Collection<String>
	 * @param propertyName String
	 * @param smallMoleculeReference SmallMoleculeReference
	 */
	private void setPubChemDatabaseLinks(String db, Collection<String> ids, String delimiter, SmallMoleculeReference smallMoleculeReference) {
		
		for (String id : ids) {
			setRelationshipXref(db + delimiter + id, delimiter, smallMoleculeReference);
		}
	}

	/**
	 * Creates the unification xrefs for the given collection.
	 *
	 * @param ids Collection<String>
	 * @param propertyName String
	 * @param smallMoleculeReference SmallMoleculeReference
	 */
	private void setUnificationXref(Collection<String> ids, String delimiter, SmallMoleculeReference smallMoleculeReference) {
		
		for (String id : ids) {
			setUnificationXref(id, delimiter, smallMoleculeReference);
		}
	}

	/**
	 * Creates a unification xref for the given id.
	 *
	 * Note: id is db + delimiter + id
	 *
	 * @param id String
	 * @param delimiter String
	 * @param smallMoleculeReference SmallMoleculeReference
	 */
	private void setUnificationXref(String id, String delimiter, SmallMoleculeReference smallMoleculeReference) {

		if (id != null) {
			String[] parts = id.split(delimiter);
			if (parts.length != 2) return;
			if (log.isInfoEnabled()) {
				log.info("setUnificationXref(), parts: " + parts[0] + ", " + parts[1]);
			}
			smallMoleculeReference.addXref(getXref(UnificationXref.class, parts));
		}
	}

	/**
	 * Creates a relationship xref for the given id.
	 *
	 * Note: id is db + delimiter + id
	 *
	 * @param id String
	 * @param delimiter String
	 * @param smallMoleculeReference SmallMoleculeReference
	 */
	private void setRelationshipXref(String id, String delimiter, SmallMoleculeReference smallMoleculeReference) {

		if (id != null) {
			String[] parts = id.split(delimiter);
			if (parts.length != 2) return;
			if (log.isInfoEnabled()) {
				log.info("setRelationshipXref(), parts: " + parts[0] + ", " + parts[1]);
			}
			smallMoleculeReference.addXref(getXref(RelationshipXref.class, parts));
		}
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

		if (model.containsID(id)) {
			toReturn = (RelationshipTypeVocabulary)model.getByID(id);
		}
		else {
			// create a new cv
			toReturn =
				(RelationshipTypeVocabulary)model.addNew(RelationshipTypeVocabulary.class, id);
			toReturn.addTerm(""); // convert dbName into some term
		}

		// outta here
		return toReturn;
	}

	/**
	 * Creates a publication xref for the given id.
	 *
	 * Note: id is db + delimiter + id
	 *
	 * @param id String
	 * @param delimiter String
	 * @param smallMoleculeReference SmallMoleculeReference
	 */
	private void setPublicationXref(String id, String delimiter, SmallMoleculeReference smallMoleculeReference) {

		if (id != null) {
			String[] parts = id.split(delimiter);
			if (parts.length != 2) return;
			if (log.isInfoEnabled()) {
				log.info("setPublicationXref(), parts: " + parts[0] + ", " + parts[1]);
			}
			smallMoleculeReference.addXref(getXref(PublicationXref.class, parts));
		}
	}

	/**
	 * Given an xref class and a string array containing a
	 * db name at pos 0 and db id at pos 1 returns a proper xref.
	 *
	 * @param parts String[]
	 * @return <T extends Xref>
	 */
	private <T extends Xref> T getXref(Class<T> aClass, String[] parts) {
		
		T toReturn = null;

		String dbName = parts[0].trim().toLowerCase();
		String dbID = parts[1].trim();
		String URI = "";
		if (aClass.getSimpleName().equals("UnificationXref")) {
			URI = BaseConverterImpl.L3_UNIFICATIONXREF_URI;
		}
		else if (aClass.getSimpleName().equals("RelationshipXref")) {
			URI = BaseConverterImpl.L3_RELATIONSHIPXREF_URI;
		}
		else if (aClass.getSimpleName().equals("PublicationXref")) {
			URI = BaseConverterImpl.L3_PUBLICATIONXREF_URI;
		}
		String rdfID =  URI + URLEncoder.encode(dbName + "_" + dbID);

		if (model.containsID(rdfID)) {
			toReturn = (T)model.getByID(rdfID);
		}
		else {
			toReturn = (T)model.addNew(aClass, rdfID);
			toReturn.setDb(dbName);
			toReturn.setId(dbID);
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
	
	
	// TODO using Miriam (MiriamLink), parse ID from the URL
	// (shelved for later; not a problem now...)
	private String getIdFromPubchemExtUrl(String pubchemExtUrl) {
		return null;
	}
}