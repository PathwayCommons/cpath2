package cpath.converter.internal;

import java.io.*;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.biopax.validator.utils.Normalizer;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import cpath.dao.PaxtoolsDAO;

/**
 * Implementation of Converter interface for ChEBI data.
 */
public class ChEBIConverterImpl extends BaseConverterImpl
{
	static enum WhatEntryToProcess {
		PROCESS_SDF,
		PROCESS_OBO;
	}
	
	//use java option -Dcpath.converter.sdf.skip to process OBO only
	public static final String JAVA_OPT_SKIP_SDF = "cpath.converter.sdf.skip";

	private static final String SDF_ENTRY_START = "M  END";
	private static final String SDF_ENTRY_END = "$$$$";
	
	private static final String CHEBI_OBO_ENTRY_START = "[Term]";
	private static final String CHEBI_OBO_ENTRY_END = "";
	
	// some statics to identify names methods
	protected static final String DISPLAY_NAME = "DISPLAY_NAME";
	protected static final String STANDARD_NAME = "STANDARD_NAME";
	protected static final String ADDITIONAL_NAME = "ADDITIONAL_NAME";

	// some statics to identify secondary id delimiters
	protected static final String COLON_DELIMITER = ":";
	protected static final String EQUALS_DELIMITER = "=";
	
	 // loader can handle file://, ftp://, http://  URL resources
	private static final ResourceLoader LOADER = new DefaultResourceLoader();
	
	// url to chebi obo file - set by ChEBI subclass
	private final String chebiOboFileUrl;
    
    // OBO converter
    private ChEBIOBOConverterImpl oboConverter;	

	// url to ChEBI OBO file
	public static final String CHEBI_OBO_FILE_URL = 
		"ftp://ftp.ebi.ac.uk/pub/databases/chebi/ontology/chebi.obo";
	// chebi obo file target path/name
	public static final String CHEBI_OBO_FILE = 
		System.getProperty("java.io.tmpdir") 
			+ System.getProperty("file.separator") + "chebi.obo";
	
	
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
	
	public ChEBIConverterImpl() {
		this(null);
	}
	
	public ChEBIConverterImpl(Model model) {
		this(model, CHEBI_OBO_FILE_URL);
		if (log.isDebugEnabled()) {
			log.debug("CHEBI OBO FILE URL: " + chebiOboFileUrl);
		}
	}

	
	/**
	 * Constructor.
	 *
	 * @param model to merge converted data to
	 */
	public ChEBIConverterImpl(Model model, String chebiOBOFileURL) {
		super(model);
		this.oboConverter = new ChEBIOBOConverterImpl(model, factory);
		this.chebiOboFileUrl = chebiOBOFileURL;
	}

	
	@Override
	public void convert(final InputStream is) {
		
		// first convert given SDF input stream and store SM in warehouse
		if(System.getProperty(JAVA_OPT_SKIP_SDF) == null) {
			convert(is, WhatEntryToProcess.PROCESS_SDF, SDF_ENTRY_START, SDF_ENTRY_END);
		}
		
		// Note - we are only converting ChEBI now, so assume OBO processing required
		InputStream oboIS = null;
		try {
			if (oboConverter.getModel() == null) {
				oboConverter.setModel(model);
			}
			oboIS = getChEBIOBOInputStream();
			convert(oboIS, WhatEntryToProcess.PROCESS_OBO, CHEBI_OBO_ENTRY_START, CHEBI_OBO_ENTRY_END);
		}
		catch (IOException e) {
			log.error("convert(): cannot create hierarchical relationships!", e);
		}
		finally {
			if (oboIS != null) {
				try {
					oboIS.close();
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
	 * Utility function that helps convert both SDF and OBO files.
	 *  
	 * @param is InputStream
	 * @param whatEntryToProcess String
	 * @param entryStart String
	 * @param entryEnd String
	 */
	private void convert(final InputStream is, 
		WhatEntryToProcess whatEntryToProcess, String entryStart, String entryEnd) 
	{
		// ref to reader here so, we can close in finally clause
        InputStreamReader reader = null;

        try {
			// setup the reader
            reader = new InputStreamReader(is, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(reader);
            if (log.isInfoEnabled()) {
            	log.info("convert(), starting to read data...");
			}
			// read the file
            String line = bufferedReader.readLine();
            while (line != null) {
				// start of entry
                if (line.startsWith(entryStart)) {
					StringBuffer entryBuffer = new StringBuffer(line + "\n");
					line = bufferedReader.readLine();
					while (line != null) {
						entryBuffer.append(line + "\n");
						// keep reading until we reach last modified
						if (whatEntryToProcess == WhatEntryToProcess.PROCESS_SDF 
							&& line.startsWith(entryEnd)) {
							break;
						}
						else if (whatEntryToProcess.equals(WhatEntryToProcess.PROCESS_OBO) 
							&& line.isEmpty()) {
							break;
						}
						line = bufferedReader.readLine();
					}
					if (whatEntryToProcess==WhatEntryToProcess.PROCESS_SDF) {
						processSDFEntry(entryBuffer);
					}
					else if (whatEntryToProcess==WhatEntryToProcess.PROCESS_OBO) {
						oboConverter.processOBOEntry(entryBuffer);
					}
                }
                line = bufferedReader.readLine();
            }
            // quick fix - mainly for testing (without DAO)
            if(!(model instanceof PaxtoolsDAO)) {
            	model.repair();
            }
        }
		catch (IOException e) {
			log.error(e);
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
    }

	/**
	 * Given a string buffer for a single SDF entry,
	 * create a BioPAX Entity reference.
	 *
	 * @param entryBuffer StringBuffer
	 * @throws IOException
	 */
	private void processSDFEntry(StringBuffer entryBuffer) throws IOException { 

        if (log.isDebugEnabled()) {
        	log.debug("calling processSDFEntry()");
        }
        // build a new SMR with its dependent elements
		SmallMoleculeReference smr = buildSmallMoleculeReference(entryBuffer);
		if (smr != null) {
            if(model instanceof PaxtoolsDAO) {
            	((PaxtoolsDAO) model).merge(smr);
            }
            else {
            	// don't merge now (later - all at once)
            	model.add(smr);
            }
		}
	}
	
	
	/**
	 * Method to download and return an InputStream to the OBO file.
	 * @return InputStream
	 * @throws IOException
	 */
	private InputStream getChEBIOBOInputStream() throws IOException 
	{
		File localFile = new File(ChEBIConverterImpl.CHEBI_OBO_FILE);
		if(!localFile.exists() || !localFile.isFile()) {
			Resource resource = LOADER.getResource(chebiOboFileUrl);
			long size = resource.contentLength();
			if(log.isInfoEnabled()) {
				log.info(CHEBI_OBO_FILE_URL + " content length= " + size);
			}
			ReadableByteChannel source = Channels.newChannel(resource.getInputStream());
			FileOutputStream dest = new FileOutputStream(ChEBIConverterImpl.CHEBI_OBO_FILE);
			size = dest.getChannel().transferFrom(source, 0, size); // can throw runtime exceptions
			if(log.isInfoEnabled()) {
				log.info(size + " bytes downloaded from " + CHEBI_OBO_FILE_URL);
			}
		} else {
			log.info("Re-using existing file: " + ChEBIConverterImpl.CHEBI_OBO_FILE);
		}

		// outta here
		return new FileInputStream(ChEBIConverterImpl.CHEBI_OBO_FILE);
	}
	
	
	/**
	 * Creates a new small molecule 
	 * reference (and other related elements in it) 
	 * from the SDF entry. 
	 *
	 * @param stringBuffer StringBuffer
	 * @throws IOException
	 */
	private SmallMoleculeReference buildSmallMoleculeReference(StringBuffer entryBuffer) 
		throws IOException 
	{	
		SmallMoleculeReference toReturn = null;
		
        // grab rdf id, create SMR 
		String rdfID = getRDFID(entryBuffer);
		if (rdfID == null) {
			return null;
		}
		toReturn = factory.create(SmallMoleculeReference.class, rdfID);
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
		String chemicalStructureID = ModelUtils.BIOPAX_URI_PREFIX 
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
			ChemicalStructure chemStruct = factory.create(ChemicalStructure.class, chemicalStructureID);
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
		
		String rdfID = Normalizer.generateURIForXref(db, id, null, aClass);

		if (model.containsID(rdfID)) {
			toReturn = getById(rdfID, aClass);
		}
		else {
			toReturn = (T) factory.create(aClass, rdfID);
			toReturn.setDb(db);
			toReturn.setId(id);
			
			// TODO doubt: should this apply for all ids?..
			if ("RelationshipXref".equals(aClass.getSimpleName())) {
				toReturn.setIdVersion("entry_name");
			}
			
			//TODO how about to add this xref to the model, huh? (otherwise, have to/will do in the SimpleMerger)
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
