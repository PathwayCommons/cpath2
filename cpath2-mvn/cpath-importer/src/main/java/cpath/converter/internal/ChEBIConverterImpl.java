package cpath.converter.internal;

import java.io.*;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.level3.*;
import org.biopax.validator.utils.Normalizer;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;


/**
 * Implementation of Converter interface for ChEBI data.
 * This converter goes over the data in two passes:
 * - first, it creates SMRs, xrefs, etc.;
 * - second, it uses ChEBI OBO ontology to set memberEntityReference properties 
 * (to represent ChEBI warehouse hierarchy in BioPAX)
 */
class ChEBIConverterImpl extends BaseConverterImpl
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
	
	 // loader can handle classpath:, file://, ftp://, http://  URL resources
	private static final ResourceLoader LOADER = new DefaultResourceLoader();
	
    
    // OBO converter
    private ChEBIOBOConverter oboConverter;	
	
	
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
	
	
	
	ChEBIConverterImpl() {
		this.oboConverter = new ChEBIOBOConverter(factory);
	}
	
	
	/**
	 * {@inheritDoc}
	 * 
	 * @param optionalArgs - the first element can be the location (URL)
	 * 						 of the chebi.obo file; others are ignored.
	 */
	@Override
	public void convert(final InputStream is, Object... optionalArgs) {
		
		// first convert given SDF input stream and store SM in warehouse
		if(System.getProperty(JAVA_OPT_SKIP_SDF) == null) {
			convert(is, WhatEntryToProcess.PROCESS_SDF, SDF_ENTRY_START, SDF_ENTRY_END);
		}
		
		String chebiOboFileUrl = "classpath:chebi.obo"; //default
		if(optionalArgs.length >= 1 && optionalArgs[0] != null)
			chebiOboFileUrl = optionalArgs[0].toString();
		
		// Note - we are only converting ChEBI now, so assume OBO processing required
		InputStream oboIS = null;
		try {
			oboIS = LOADER.getResource(chebiOboFileUrl).getInputStream();
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
            String line;
            while ((line = bufferedReader.readLine()) != null) {
				// start of entry
                if (line.startsWith(entryStart)) {
					StringBuilder entryBuffer = new StringBuilder(line + "\n");
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
				       	log.debug("calling processSDFEntry()");
				        // build a new SMR with its dependent elements
						buildSmallMoleculeReference(entryBuffer);
					}
					else if (whatEntryToProcess==WhatEntryToProcess.PROCESS_OBO) {
						oboConverter.processOBOEntry(entryBuffer);
					}
                }
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
	 * Creates a new small molecule 
	 * reference (and other related elements in it) 
	 * from the SDF entry. 
	 *
	 * @param StringBuilder StringBuilder
	 * @throws IOException
	 */
	private SmallMoleculeReference buildSmallMoleculeReference(StringBuilder entryBuffer) 
		throws IOException 
	{	
		SmallMoleculeReference toReturn = null;
		
        // grab rdf id, create SMR 
		// line contains id in form: CHEBI:15377
		String chebiId = getValue(entryBuffer, CHEBI_ID);
		if (chebiId == null) 
			return null;
		
		String rdfID = "http://identifiers.org/obo.chebi/" + chebiId;
		toReturn = (SmallMoleculeReference) model.getByID(rdfID);
		if(toReturn != null)
			return toReturn;
		
		// create a new SMR
		toReturn = model.addNew(SmallMoleculeReference.class, rdfID);
		// primary id (u.xref)
		toReturn.addXref(getXref(UnificationXref.class, chebiId, "ChEBI"));
		// names
		setName(getValue(entryBuffer, CHEBI_NAME), DISPLAY_NAME, toReturn);
		setName(getValue(entryBuffer, CHEBI_NAME), STANDARD_NAME, toReturn);
		// comment
		setComment(getValue(entryBuffer, CHEBI_DEFINITION), toReturn);
		// secondary ids
		for (String id : getValues(entryBuffer, CHEBI_SECONDARY_ID)) {
			toReturn.addXref(getXref(UnificationXref.class, id, "ChEBI"));
		}		

		String chemicalStructureID = Normalizer.uri(model.getXmlBase(), "chebi", chebiId, ChemicalStructure.class);

		String structure = getValue(entryBuffer, CHEBI_INCHI);
		setChemicalStructure(structure, StructureFormatType.InChI, chemicalStructureID, toReturn);
		// inchi key unification xref
		String inchiKey = getValue(entryBuffer, CHEBI_INCHI_KEY);
		if (inchiKey != null && inchiKey.length() > 0) {
			String id = inchiKey.split(EQUALS_DELIMITER)[1];
			toReturn.addXref(getXref(UnificationXref.class, id, "InChIKey"));
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
		
		return toReturn;
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
	 * @param structureFormat
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
			ChemicalStructure chemStruct = model.addNew(ChemicalStructure.class, chemicalStructureID);
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
	 * @param entry StringBuilder
	 * @param registryPropKey String
	 * @param smallMoleculeReference SmallMoleculeReference
	 * @throws IOException
	 */
	private void setChEBIRegistryNumbers(StringBuilder entry, String registryPropKey, SmallMoleculeReference smallMoleculeReference) throws IOException {

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
	 * @param entry StringBuilder
	 * @param databaseRegex String
	 * @param smallMoleculeReference SmallMoleculeReference
	 * @throws IOException
	 */
	private void setChEBIDatabaseLinks(StringBuilder entry, String databaseRegex, 
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
							// add two unification xrefs
							// the first uses umbiguous name (as is), not standard, but is helpful for id-mapping 
							smallMoleculeReference.addXref(getXref(UnificationXref.class, id, "pubchem"));
							// the second one is correct (is what it means in chebi - pubchem substance ref)
							smallMoleculeReference.addXref(getXref(UnificationXref.class, id, "pubchem-substance"));	
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
		
		String rdfID = Normalizer.uri(model.getXmlBase(), db, id, aClass);
		
		toReturn = (T) model.getByID(rdfID);
		
		if(toReturn == null) {
			toReturn = (T) model.addNew(aClass, rdfID);
			toReturn.setDb(db);
			toReturn.setId(id);
			
			// TODO doubt: should this apply for all ids?..
			if ("RelationshipXref".equals(aClass.getSimpleName())) {
				toReturn.setIdVersion("entry_name");
			}
		}

		return toReturn;
	}

	/**
	 * Given an SDF entry, searches for given key and returns 
	 * value associated with that key.
	 *
	 * @param entry StringBuilder
	 * @param key String
	 * @return String
	 * @throws IOException
	 */
	private String getValue(StringBuilder entry, String key) throws IOException {
		
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
	 * @param entry StringBuilder
	 * @param key String
	 * @return Collection<String>
	 * @throws IOException
	 */
	private Collection<String> getValues(StringBuilder entry, String key) 
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
	 * @param entry StringBuilder
	 * @return BufferedReader
	 * @throws IOException
	 */
	private BufferedReader getBufferedReader(StringBuilder entry) throws IOException {
		return new BufferedReader (new StringReader(entry.toString()));
	}
	
	
	private class ChEBIOBOConverter {
	    private final Log log = LogFactory.getLog(ChEBIOBOConverter.class);
	    
		private final Pattern CHEBI_OBO_ID_REGEX = Pattern.compile("^id: CHEBI:(\\w+)$");
		private final Pattern CHEBI_OBO_ISA_REGEX = Pattern.compile("^is_a: CHEBI:(\\w+)$");
		private final Pattern CHEBI_OBO_RELATIONSHIP_REGEX = Pattern.compile("^relationship: (\\w+) CHEBI:(\\w+)$");
		private final String REGEX_GROUP_DELIMITER = ":";
	    	
		private BioPAXFactory factory;
			
		/**
		 * Constructor.
		 * @param factory BioPAXFactory
		 */
		ChEBIOBOConverter(BioPAXFactory factory) {
			this.factory = factory;
		}
		
		
		/**
		 * Given a ChEBI - OBO entry, creates proper member entity reference
		 * between parent and child.
		 * 
		 * @param entryBuffer
		 * @throws IOException
		 */
		public void processOBOEntry(StringBuilder entryBuffer) throws IOException 
		{
			if (log.isDebugEnabled()) {
				log.debug("calling processOBOEntry()");
			}
			// get SMR for entry out of warehouse
			Collection<String> childChebiIDs = getValuesByREGEX(entryBuffer, CHEBI_OBO_ID_REGEX);
			if (childChebiIDs.size() != 1) {
				if (log.isDebugEnabled()) {
					log.debug("processOBOEntry(), problem parsing 'id:' tag for this entry: " + entryBuffer.toString());
					log.debug("processOBOEntry(), returning...");
				}
				return;
			}
			SmallMoleculeReference childSMR = getSMRByChebiID(childChebiIDs.iterator().next());
			if (childSMR == null) {
				if (log.isDebugEnabled()) {
					log.debug("processOBOEntry(), Cannot find SMR by ChebiID for this entry: " + entryBuffer.toString());
					log.debug("processOBOEntry(), returning...");
				}
				return;
			}
			
			// for each parent ChEBI, create a member entity reference to child
			Collection<String> parentChebiIDs = getValuesByREGEX(entryBuffer, CHEBI_OBO_ISA_REGEX);
			for (String parentChebiID : parentChebiIDs) {
				SmallMoleculeReference parentSMR = getSMRByChebiID(parentChebiID);
				if (parentSMR == null) {
					if (log.isDebugEnabled()) {
						log.debug("processOBOEntry(), Cannot find SMR by ChebiID via 'is_a', entry: " + entryBuffer.toString());
						log.debug("processOBOEntry(), skipping...");
					}
					continue;
				}
				parentSMR.addMemberEntityReference(childSMR);
			}

			// we can also grab relationship (horizontal hierarchical info) 
			Collection<String> relationships = getValuesByREGEX(entryBuffer, CHEBI_OBO_RELATIONSHIP_REGEX);
			for (String relationship : relationships) {
				String[] parts = relationship.split(REGEX_GROUP_DELIMITER);
				RelationshipXref xref = getRelationshipXref(parts[0].toLowerCase(), parts[1]); 
				childSMR.addXref(xref);
			}
		}
		
		/**
		 * Given an OBO entry, returns the values matched by the given regex.
		 * If regex contains more that one capture group, a ":" will be used to delimit them.
		 *  
		 * @param entryBuffer StringBuilder
		 * @param regex Pattern
		 * @return String
		 * @throws IOException
		 */
		private Collection<String> getValuesByREGEX(StringBuilder entryBuffer, Pattern regex) throws IOException {
			
			Collection<String> toReturn = new ArrayList<String>();
			BufferedReader reader = getBufferedReader(entryBuffer);

			if (log.isDebugEnabled()) {
				log.debug("getValue(), key: " + regex.toString());
			}

			String line = reader.readLine();
			while (line != null) {
				Matcher matcher = regex.matcher(line);
				if (matcher.find()) {
					String toAdd = "";
					for (int lc = 1; lc <= matcher.groupCount(); lc++) {
						toAdd += matcher.group(lc) + REGEX_GROUP_DELIMITER;
					}
					toReturn.add(toAdd.substring(0, toAdd.length()-1));
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
		 * Given a ChEBI Id, returns the matching SMR in the warehouse.
		 * 
		 * @param chebiID String
		 * @return SmallMoleculeReference
		 */
		private SmallMoleculeReference getSMRByChebiID(String chebiID) {
			String rdfID = "http://identifiers.org/obo.chebi/CHEBI:" + chebiID;
			return (SmallMoleculeReference) model.getByID(rdfID);
		}
		
		/**
		 * Given a relationship from a ChEBI OBO file, returns a relationship xref.
		 * 
		 * @param relationshipType String
		 * @param chebiID String
		 */
		private RelationshipXref getRelationshipXref(String relationshipType, String chebiID) {
			
			RelationshipXref toReturn = null;
			
			// We use the relationship type in the URI of xref since there can be many to many relation types
			// bet SM.  For example CHEBI:X has_part CHEBI:Y and CHEBI:Z is_conjugate_acid_of CHEBI:Y
			// we need distinct rxref to has_part CHEBI:Y and is_conjugate_acid_of CHEBI:Y
			String xrefRdfID = Normalizer.uri(model.getXmlBase(), 
					"CHEBI", "CHEBI:"+chebiID+relationshipType, RelationshipXref.class);
			
			if (model.containsID(xrefRdfID)) {
				return (RelationshipXref) model.getByID(xrefRdfID);
			}
			
			// made it here, need to create relationship xref
			toReturn = model.addNew(RelationshipXref.class, xrefRdfID);
			toReturn.setDb("CHEBI");
			toReturn.setId("CHEBI:"+chebiID);
			
			// set relationship type vocabulary on the relationship xref
			String relTypeRdfID = 
				Normalizer.uri(model.getXmlBase(), null, relationshipType, RelationshipTypeVocabulary.class);		
			
			RelationshipTypeVocabulary rtv = (RelationshipTypeVocabulary) model.getByID(relTypeRdfID);
			if (rtv != null) {
				toReturn.setRelationshipType(rtv);
			} else {
				rtv = model.addNew(RelationshipTypeVocabulary.class, relTypeRdfID);
				rtv.addTerm(relationshipType);
				toReturn.setRelationshipType(rtv);
			}
			
			return toReturn;
		}
		
		/**
		 * Given a string buffer representation of an entry,
		 * returns a buffered reader to the entry.
		 *
		 * @param entry StringBuilder
		 * @return BufferedReader
		 * @throws IOException
		 */
		private BufferedReader getBufferedReader(StringBuilder entry) throws IOException {
			return new BufferedReader (new StringReader(entry.toString()));
		}
	}
}
