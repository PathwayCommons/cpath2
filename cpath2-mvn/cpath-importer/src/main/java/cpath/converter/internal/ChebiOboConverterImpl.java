package cpath.converter.internal;

import java.io.*;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.model.level3.*;
import org.biopax.validator.utils.Normalizer;


/**
 * Implementation of Converter interface 
 * for ChEBI ontology (OBO) data.
 * 
 * This goes after ChEBI SDF data has been already processed!
 * 
 * It uses ChEBI OBO ontology to set memberEntityReference 
 * properties (to represent ChEBI warehouse hierarchy in BioPAX)
 */
class ChebiOboConverterImpl extends BaseConverterImpl
{
	
	//use java option -Dcpath.converter.sdf.skip to process OBO only
	public static final String JAVA_OPT_SKIP_SDF = "cpath.converter.sdf.skip";
	
	private static final String CHEBI_OBO_ENTRY_START = "[Term]";
	
	// some statics to identify names methods
	protected static final String DISPLAY_NAME = "DISPLAY_NAME";
	protected static final String STANDARD_NAME = "STANDARD_NAME";
	protected static final String ADDITIONAL_NAME = "ADDITIONAL_NAME";

	// some statics to identify secondary id delimiters
	protected static final String COLON_DELIMITER = ":";
	protected static final String EQUALS_DELIMITER = "=";	
	
    private static Log log = LogFactory.getLog(ChebiOboConverterImpl.class);
	  
	private final Pattern CHEBI_OBO_ID_REGEX = Pattern.compile("^id: CHEBI:(\\w+)$");
	private final Pattern CHEBI_OBO_ISA_REGEX = Pattern.compile("^is_a: CHEBI:(\\w+)$");
	private final Pattern CHEBI_OBO_RELATIONSHIP_REGEX = Pattern.compile("^relationship: (\\w+) CHEBI:(\\w+)$");
	private final String REGEX_GROUP_DELIMITER = ":";
	
	
	/**
	 * {@inheritDoc}
	 * @throws IOException 
	 */
	@Override
	public void convert(final InputStream is) {				
		log.info("convert(), starting to read data...");
		
		if(is == null)
			throw new IllegalArgumentException("The second parameter must be not null input stream");
		
		try {
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(is, "UTF-8"));			
			
			String line;
			while ((line = reader.readLine()) != null) {
				// start of entry
				if (line.startsWith(CHEBI_OBO_ENTRY_START)) {
					StringBuilder entryBuffer = new StringBuilder(line + "\n");
					line = reader.readLine();
					while (line != null) {
						entryBuffer.append(line + "\n");
						// keep reading until we reach last modified
						if (line.isEmpty())
							break;
						line = reader.readLine();
					}

					processOBOEntry(entryBuffer);
				}
			}

			if (reader != null)
				reader.close();
		} catch (IOException e) {
			log.warn("is.close() failed." + e);
		}
		
		log.info("convert(), exiting.");
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
		
		log.debug("getXref(), id: " + id + ", db: " + db 
				+ ", type: " + aClass.getSimpleName());
		
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
	
	/**
	 * Given a ChEBI - OBO entry, creates proper member entity reference between
	 * parent and child.
	 * 
	 * @param entryBuffer
	 * @throws IOException
	 */
	public void processOBOEntry(StringBuilder entryBuffer) throws IOException {
		if (log.isDebugEnabled()) {
			log.debug("calling processOBOEntry()");
		}
		// get SMR for entry out of warehouse
		Collection<String> childChebiIDs = getValuesByREGEX(entryBuffer,
				CHEBI_OBO_ID_REGEX);
		if (childChebiIDs.size() != 1) {
			if (log.isDebugEnabled()) {
				log.debug("processOBOEntry(), problem parsing 'id:' tag for this entry: "
						+ entryBuffer.toString());
				log.debug("processOBOEntry(), returning...");
			}
			return;
		}
		SmallMoleculeReference childSMR = getSMRByChebiID(childChebiIDs
				.iterator().next());
		if (childSMR == null) {
			if (log.isDebugEnabled()) {
				log.debug("processOBOEntry(), Cannot find SMR by ChebiID for this entry: "
						+ entryBuffer.toString());
				log.debug("processOBOEntry(), returning...");
			}
			return;
		}

		// for each parent ChEBI, create a member entity reference to child
		Collection<String> parentChebiIDs = getValuesByREGEX(entryBuffer,
				CHEBI_OBO_ISA_REGEX);
		for (String parentChebiID : parentChebiIDs) {
			SmallMoleculeReference parentSMR = getSMRByChebiID(parentChebiID);
			if (parentSMR == null) {
				if (log.isDebugEnabled()) {
					log.debug("processOBOEntry(), Cannot find SMR by ChebiID via 'is_a', entry: "
							+ entryBuffer.toString());
					log.debug("processOBOEntry(), skipping...");
				}
				continue;
			}
			parentSMR.addMemberEntityReference(childSMR);
		}

		// we can also grab relationship (horizontal hierarchical info)
		Collection<String> relationships = getValuesByREGEX(entryBuffer,
				CHEBI_OBO_RELATIONSHIP_REGEX);
		for (String relationship : relationships) {
			String[] parts = relationship.split(REGEX_GROUP_DELIMITER);
			RelationshipXref xref = getRelationshipXref(parts[0].toLowerCase(),
					parts[1]);
			childSMR.addXref(xref);
		}
	}

	/**
	 * Given an OBO entry, returns the values matched by the given regex. If
	 * regex contains more that one capture group, a ":" will be used to delimit
	 * them.
	 * 
	 * @param entryBuffer
	 * @param regex
	 * @return
	 * @throws IOException
	 */
	private Collection<String> getValuesByREGEX(StringBuilder entryBuffer,
			Pattern regex) throws IOException {

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
				toReturn.add(toAdd.substring(0, toAdd.length() - 1));
			}
			line = reader.readLine();
		}

		return toReturn;
	}

	/**
	 * Given a ChEBI Id, returns the matching SMR in the warehouse.
	 * 
	 * @param chebiID
	 * @return SmallMoleculeReference
	 */
	private SmallMoleculeReference getSMRByChebiID(String chebiID) {
		String rdfID = "http://identifiers.org/chebi/CHEBI:" + chebiID;
		return (SmallMoleculeReference) model.getByID(rdfID);
	}

	/**
	 * Given a relationship from a ChEBI OBO file, returns a relationship xref.
	 * 
	 * @param relationshipType
	 * @param chebiID
	 */
	private RelationshipXref getRelationshipXref(String relationshipType,
			String chebiID) {

		RelationshipXref toReturn = null;

		// We use the relationship type in the URI of xref since there can be
		// many to many relation types
		// bet SM. For example CHEBI:X has_part CHEBI:Y and CHEBI:Z
		// is_conjugate_acid_of CHEBI:Y
		// we need distinct rxref to has_part CHEBI:Y and is_conjugate_acid_of
		// CHEBI:Y
		String xrefRdfID = Normalizer.uri(model.getXmlBase(), "CHEBI", "CHEBI:"
				+ chebiID + relationshipType, RelationshipXref.class);

		if (model.containsID(xrefRdfID)) {
			return (RelationshipXref) model.getByID(xrefRdfID);
		}

		// made it here, need to create relationship xref
		toReturn = model.addNew(RelationshipXref.class, xrefRdfID);
		toReturn.setDb("CHEBI");
		toReturn.setId("CHEBI:" + chebiID);

		// set relationship type vocabulary on the relationship xref
		String relTypeRdfID = Normalizer.uri(model.getXmlBase(), null,
				relationshipType, RelationshipTypeVocabulary.class);

		RelationshipTypeVocabulary rtv = (RelationshipTypeVocabulary) model
				.getByID(relTypeRdfID);
		if (rtv != null) {
			toReturn.setRelationshipType(rtv);
		} else {
			rtv = model.addNew(RelationshipTypeVocabulary.class, relTypeRdfID);
			rtv.addTerm(relationshipType);
			toReturn.setRelationshipType(rtv);
		}

		return toReturn;
	}

}
