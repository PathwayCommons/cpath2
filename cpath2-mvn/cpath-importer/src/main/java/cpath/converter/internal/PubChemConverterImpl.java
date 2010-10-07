package cpath.converter.internal;

import java.io.*;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;

/**
 * Implementation of Converter interface for PubChem data.
 */
public class PubChemConverterImpl extends BaseSDFConverterImpl {
	// logger
    private static Log log = LogFactory.getLog(PubChemConverterImpl.class);

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

	
	public PubChemConverterImpl() {
		this(null);
	}
	
	public PubChemConverterImpl(Model model) {
		super(model);
	}
	
	/**
	 * Given an SDF entry, return a small molecule reference.
	 *
	 * @param stringBuffer StringBuffer
	 * @throws IOException
	 */
	@Override
	public SmallMoleculeReference buildSmallMoleculeReference(StringBuffer entryBuffer) 
		throws IOException 
	{
		SmallMoleculeReference toReturn = null;

        // rdf id / small molecule ref 
		String rdfID = getRDFID(entryBuffer);
		if (rdfID == null) 
			return null;
		
		// do not import any entity reference that does not have a smiles entry
		String smiles = getValue(entryBuffer, PUBCHEM_OPENEYE_CAN_SMILES);
		if (smiles == null || smiles.length() == 0) {
			if(log.isInfoEnabled())
				log.info("Skipping PubChem entry without smiles, id: " 
						+ rdfID);
			return null;
		}
		
		SmallMoleculeReference smallMoleculeReference =
			factory.reflectivelyCreate(SmallMoleculeReference.class);
		smallMoleculeReference.setRDFId(rdfID);

		// chemical formula
		setChemicalFormula(getValue(entryBuffer, PUBCHEM_MOLECULAR_FORMULA), smallMoleculeReference);
		// names
		setName(getValue(entryBuffer, PUBCHEM_IUPAC_NAME), DISPLAY_NAME, smallMoleculeReference);
		setName(getValue(entryBuffer, PUBCHEM_IUPAC_TRADITIONAL_NAME), STANDARD_NAME, smallMoleculeReference);
		setName(getValue(entryBuffer, PUBCHEM_IUPAC_OPENEYE_NAME), ADDITIONAL_NAME, smallMoleculeReference);
		setName(getValue(entryBuffer, PUBCHEM_IUPAC_CAS_NAME), ADDITIONAL_NAME, smallMoleculeReference);
		setName(getValue(entryBuffer, PUBCHEM_IUPAC_SYSTEMATIC_NAME), ADDITIONAL_NAME, smallMoleculeReference);
		// smiles - chemical structure
		String[] rdfIDParts = smallMoleculeReference.getRDFId().split(COLON_DELIMITER);
		String chemicalStructureID = BaseConverterImpl.BIOPAX_URI_PREFIX 
			+ "ChemicalStructure:" + rdfIDParts[2]+"_"+rdfIDParts[3];
		setChemicalStructure(smiles, StructureFormatType.SMILES, 
				chemicalStructureID, smallMoleculeReference);
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
				smallMoleculeReference.addXref(getXref(UnificationXref.class, id, db));
			}
		}
		// pubmed
		for (String id : getValues(entryBuffer, PUBCHEM_PUBMED_ID)) {
			setPublicationXref(id, PUBMED, smallMoleculeReference);
		}
		// genbank - nucleotide
		for (String id : getValues(entryBuffer, PUBCHEM_GENBANK_NUCLEOTIDE_ID)) {
			String[] genbankNucParts = { GENBANK, id };
			RelationshipXref xref = (RelationshipXref)getXref(RelationshipXref.class, genbankNucParts);
			xref.setIdVersion(GENBANK_NUCLEOTIDE);
			smallMoleculeReference.addXref(xref);
		}
		// genbank - protein
		for (String id : getValues(entryBuffer, PUBCHEM_GENBANK_PROTEIN_ID)) {
			String[] genbankProParts = { GENBANK, id };
			RelationshipXref xref = (RelationshipXref)getXref(RelationshipXref.class, genbankProParts);
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
			
		String inchiKey = getValue(entryBuffer, PUBCHEM_NIST_INCHIKEY);
		String inchi = getValue(entryBuffer, PUBCHEM_NIST_INCHI);
		if (inchiKey != null && inchiKey.length() > 0) {
			String key = inchiKey.split(EQUALS_DELIMITER)[1]; // got OutOfBoundariesException here with real pubchem data :(
			toReturn = getInchiEntityReference(key, inchi);
			// pubchem SMR becomes member ER of the 'inchi' one
			toReturn.addMemberEntityReference(smallMoleculeReference);
		}
		
		return toReturn; //(toReturn==null) ? smallMoleculeReference : toReturn;
		// skip (null) when without InChiKey!
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
		String id = getValue(entry, PUBCHEM_COMPOUND_ID);
		if(id != null) {
			rdfID = "urn:miriam:pubchem.compound:" + id;
		} else {
			id = getValue(entry, PUBCHEM_SUBSTANCE_ID);
			if (id == null) 
				return null;
			rdfID = "urn:miriam:pubchem.substance:" + id;
		}

		if (log.isDebugEnabled()) {
			log.debug("getRDFID(), rdfID: " + rdfID);
		}

		// outta here
		return rdfID;
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
	 * Sets pubchem database links (relationship xrefs).
	 *
	 * @param db String
	 * @param ids Collection<String>
	 * @param propertyName String
	 * @param smallMoleculeReference SmallMoleculeReference
	 */
	private void setPubChemDatabaseLinks(String db, Collection<String> ids, 
			String delimiter, SmallMoleculeReference smallMoleculeReference) 
	{	
		for (String id : ids) {
			setRelationshipXref(id, db, smallMoleculeReference);
		}
	}

	/**
	 * Creates a relationship xref for the given id.
	 *
	 * @param id String
	 * @param db String
	 * @param smallMoleculeReference SmallMoleculeReference
	 */
	private void setRelationshipXref(String id, String db, SmallMoleculeReference smallMoleculeReference) 
	{
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
	 * Creates a publication xref for the given id.
	 *
	 * @param id String
	 * @param db String
	 * @param smallMoleculeReference SmallMoleculeReference
	 */
	private void setPublicationXref(String id, String db, SmallMoleculeReference smallMoleculeReference) {

		if (log.isDebugEnabled()) {
			log.debug("setPublicationXref(), id, db: " + id + ", " + db);
		}
		smallMoleculeReference.addXref(getXref(PublicationXref.class, id, db));
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
	private String getValue(StringBuffer entry, String key) throws IOException 
	{
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
