package cpath.converter.internal;

import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.net.URLEncoder;

/**
 * Base Implementation of Converter interface for SDF (ChEBI & PubChem) data.
 */
public abstract class BaseSDFConverterImpl extends BaseConverterImpl {

	private static final String ENTRY_START = "M  END";
	private static final String ENTRY_END = "$$$$";
	
	// some statics to identify names methods
	protected static final String DISPLAY_NAME = "DISPLAY_NAME";
	protected static final String STANDARD_NAME = "STANDARD_NAME";
	protected static final String ADDITIONAL_NAME = "ADDITIONAL_NAME";

	// some statics to identify secondary id delimiters
	protected static final String COLON_DELIMITER = ":";
	protected static final String EQUALS_DELIMITER = "=";
	
	// logger
    private static Log log = LogFactory.getLog(BaseSDFConverterImpl.class);

	public BaseSDFConverterImpl() {
		this(null);
	}
	
	/**
	 * Constructor.
	 *
	 * @param model to merge converted data to
	 */
	public BaseSDFConverterImpl(Model model) 
	{
		super(model);
	}
	
	/**
	 * Creates a new small molecule 
	 * reference (and other related elements in it) 
	 * from the SDF entry.
	 * 
	 * By convention, this must be based on InChi/InChiKey
	 * and may contain other SMR (chebi, pubchem) 
	 * as its member entity references.
	 * 
	 * @param entry
	 * @return TODO
	 * @throws IOException
	 */
	protected abstract SmallMoleculeReference buildSmallMoleculeReference(StringBuffer entry)
		throws IOException;
	

	@Override
	public void convert(final InputStream is) {

		// ref to reader here so, we can close in finally clause
        InputStreamReader reader = null;

        try {
			// setup the reader
            reader = new InputStreamReader(is);
            BufferedReader bufferedReader = new BufferedReader(reader);
            if (log.isInfoEnabled()) {
            	log.info("convert(), starting to read data...");
			}
			// read the file
            String line = bufferedReader.readLine();
            while (line != null) {
				// start of entry
                if (line.startsWith(ENTRY_START)) {
					StringBuffer entryBuffer = new StringBuffer(line + "\n");
					line = bufferedReader.readLine();
					while (line != null) {
						entryBuffer.append(line + "\n");
						// keep reading until we reach last modified
						if (line.startsWith(ENTRY_END)) {
							break;
						}
						line = bufferedReader.readLine();
					}
					// process entry
					processEntry(entryBuffer);
                }
                line = bufferedReader.readLine();
            }
        }
		catch (IOException e) {
			log.error("Failed", e);
		}
		finally {
			if (log.isInfoEnabled()) {
				log.info("convert(), closing reader.");
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

	/**
	 * Given a string buffer for a single SDF entry,
	 * create a BioPAX Entity reference.
	 *
	 * @param entryBuffer StringBuffer
	 * @throwns IOException
	 */
	private void processEntry(StringBuffer entryBuffer) throws IOException 
	{
        if (log.isDebugEnabled())
        	log.debug("calling processEntry()");
        // build a new SMR with its dependent elements
		SmallMoleculeReference smr = buildSmallMoleculeReference(entryBuffer);
		// extract consistent sub-model from the SMR
		if (smr != null) {
			Model m = factory.createModel();
			fetcher.fetch(smr, m);
			// merge into the warehouse
			model.merge(m);
		} else {
			log.warn("Could not create (InChi) entity reference from this SDF ");
		}
	}
	
	
	/**
	 * Given a database link db, returns proper relationship type.
	 *
	 * @param dbName String
	 * @return RelationshipTypeVocabulary
	 */
	protected RelationshipTypeVocabulary getRelationshipType(String dbName) 
	{
		RelationshipTypeVocabulary toReturn;

		// check if vocabulary already exists
		String id = ""; // convert dbName into some id

		if (log.isDebugEnabled())
			log.debug("getRelationshipType(), id: " + id);

		if (model.containsID(id)) {
			toReturn = getById(id, RelationshipTypeVocabulary.class);
		} else {
			// create a new cv
			toReturn = factory.reflectivelyCreate(RelationshipTypeVocabulary.class);
			toReturn.setRDFId(id);
			toReturn.addTerm(""); // convert dbName into some term
		}

		// outta here
		return toReturn;
	}
	
	/**
	 * Given an xref class and a string array containing a
	 * db name at pos 0 and db id at pos 1 returns a proper xref.
	 *
	 * @param parts
	 * @return <T extends Xref>
	 */
	protected <T extends Xref> T getXref(Class<T> aClass, String... parts) 
	{	
		T toReturn = null;

		String id = parts[0].trim();
		String db = parts[1].trim();
		
		if (log.isDebugEnabled()) {
			log.debug("getXref(), id: " + id + ", db: " + db 
					+ ", type: " + aClass.getSimpleName());
		}
		
		String rdfID =  BaseConverterImpl.BIOPAX_URI_PREFIX +
			aClass.getSimpleName() + ":" + 
			URLEncoder.encode(db.toUpperCase() + "_" + id);

		if (model.containsID(rdfID)) {
			toReturn = getById(rdfID, aClass);
		} else {
			toReturn = (T) factory.reflectivelyCreate(aClass);
			toReturn.setRDFId(rdfID);
			toReturn.setDb(db);
			toReturn.setId(id);
			
			// TODO doubt: should this apply for all ids?..
			if ("RelationshipXref".equals(aClass.getSimpleName())) {
				toReturn.setIdVersion("entry_name");
			}
		}

		// outta here
		return toReturn;
	}
	
	/**
	 * Sets the structure.
	 *
	 * @param structure String
	 * @param structureFormat TODO
	 * @param chemicalStructureID String
	 * @param smallMoleculeReference SmallMoleculeReference
	 */
	protected void setChemicalStructure(String structure, 
			StructureFormatType structureFormat, 
			String chemicalStructureID, SmallMoleculeReference  smallMoleculeReference) 
	{	
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

	
	/*
	 * Get or create a 'inchi' small mol. ref. and make the pubchem/chebi
	 * its member entity ref. 
	 */
	protected SmallMoleculeReference getInchiEntityReference(String inchiKey, String inchi) 
	{
		SmallMoleculeReference inchiEntityRef = null;
		
		if (inchiKey != null && inchiKey.length() > 0) {
			String inchiEntityReferenceID = BaseConverterImpl.BIOPAX_URI_PREFIX 
				+ inchiKey;
			try {
				//try to pull the existing entity reference
				if (model.containsID(inchiEntityReferenceID)) {
					inchiEntityRef = getById(inchiEntityReferenceID, SmallMoleculeReference.class);
				} else {
					inchiEntityRef = factory.reflectivelyCreate(SmallMoleculeReference.class);
					inchiEntityRef.setRDFId(inchiEntityReferenceID);
					// create chem struct using inchi
					if (inchi != null && inchi.length() > 0) {
						String chemicalStructureID = BaseConverterImpl.BIOPAX_URI_PREFIX 
							+ "ChemicalStructure:" + inchiKey;
						setChemicalStructure(inchi, StructureFormatType.InChI, 
								chemicalStructureID, inchiEntityRef);
					}
				}
			}
			catch (org.biopax.paxtools.util.IllegalBioPAXArgumentException e) {
				// ignore
			}
		}
		
		return inchiEntityRef;
	}
	
}
