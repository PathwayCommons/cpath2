package cpath.converter.internal;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.biopax.validator.utils.Normalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpath.dao.Analysis;


/**
 * Implementation of Converter interface 
 * for ChEBI ontology (OBO) data.
 * 
 * This goes after ChEBI SDF data has been already processed.
 * 
 * It uses ChEBI OBO ontology to set memberEntityReference 
 * properties (to represent ChEBI warehouse hierarchy in BioPAX)
 */
class ChebiOboConverterImpl extends BaseConverterImpl implements Analysis
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
	
    private static Logger log = LoggerFactory.getLogger(ChebiOboConverterImpl.class);
	  
	private final Pattern CHEBI_OBO_ID_REGEX = Pattern.compile("^id: CHEBI:(\\w+)$");
	private final Pattern CHEBI_OBO_ISA_REGEX = Pattern.compile("^is_a: CHEBI:(\\w+)$");
	private final Pattern CHEBI_OBO_RELATIONSHIP_REGEX = Pattern.compile("^relationship: (\\w+) CHEBI:(\\w+)$");
	private final String REGEX_GROUP_DELIMITER = ":";
	
	
	/**
	 * This method is intentionally not implemented.
	 * This Converter class does not add new objects to
	 * the Warehouse model, but does modify it. 
	 * Instead calling this method, cPath2 uses 
	 * {@link #execute(Model)} interface, i.e., 
	 * calls via PaxtoolsDAO.run(analysis). 
	 */
	@Override
	public Model convert() {				
		throw new UnsupportedOperationException();
	}
	

	/**
	 * Sets the structure.
	 *
	 * @param structure String
	 * @param structureFormat
	 * @param chemicalStructureID String
	 * @param smallMoleculeReference SmallMoleculeReference
	 * @param model biopax warehouse model
	 */
	protected void setChemicalStructure(String structure, StructureFormatType structureFormat, 
			String chemicalStructureID, SmallMoleculeReference  smallMoleculeReference, Model model) {

		if (structure != null) {
			if (log.isDebugEnabled()) {
				log.debug("setStructure(), structure: " + structure);
			}
			// should only get one of these
			ChemicalStructure chemStruct = model.addNew(ChemicalStructure.class, chemicalStructureID);
			chemStruct.setStructureData(structure);
			chemStruct.setStructureFormat(structureFormat);
			smallMoleculeReference.setStructure(chemStruct);
		}
	}
	
	
	/**
	 * Given a ChEBI - OBO entry, creates proper member entity reference between
	 * parent and child.
	 * 
	 * @param entryBuffer
	 * @param model existing biopax Warehouse model
	 * @throws IOException
	 */
	public void processOBOEntry(StringBuilder entryBuffer, Model model) throws IOException {
		if (log.isDebugEnabled()) {
			log.debug("calling processOBOEntry()");
		}
		// get SMR for entry out of Warehouse
		Collection<String> childChebiIDs = getValuesByREGEX(entryBuffer, CHEBI_OBO_ID_REGEX);
		if (childChebiIDs.size() != 1) {
			log.warn("processOBOEntry(), got none or >1 id in: "
				+ entryBuffer.toString());
			return;
		}
		final String childID = childChebiIDs.iterator().next();
		SmallMoleculeReference childSMR = (SmallMoleculeReference) model
			.getByID("http://identifiers.org/chebi/CHEBI:" + childID);
		if (childSMR == null) {
			log.info("processOBOEntry(), Skipped (not found): " + childID);
			return;
		}

		// for each parent ChEBI, create a member entity reference to child
		Collection<String> parentChebiIDs = getValuesByREGEX(entryBuffer, CHEBI_OBO_ISA_REGEX);
		for (String parentChebiID : parentChebiIDs) {
			SmallMoleculeReference parentSMR = (SmallMoleculeReference) model
				.getByID("http://identifiers.org/chebi/CHEBI:" + parentChebiID);
			if (parentSMR == null) {
				log.warn("processOBOEntry(), " + childID 
					+ " IS_A " + parentChebiID + ", but "
					+ " it's not found");
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
					parts[1], model);
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
		BufferedReader reader = new BufferedReader (new StringReader(entryBuffer.toString()));

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
	 * Given a relationship from a ChEBI OBO file, 
	 * returns a relationship xref.
	 * 
	 * @param relationshipType
	 * @param chebiID
	 * @param model warehouse biopax model
	 */
	private RelationshipXref getRelationshipXref(String relationshipType,
			String chebiID, Model model) {

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


	@Override
	public void execute(Model model) {
		log.info("convert(), starting to read data...");
		
		if(inputStream == null)
			throw new IllegalArgumentException("The second parameter must be not null input stream");
		
		try {
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(inputStream, "UTF-8"));			
			
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

					processOBOEntry(entryBuffer, model);
				}
			}

			if (reader != null)
				reader.close();
		} catch (IOException e) {
			log.warn("is.close() failed." + e);
		}
		
		log.info("convert(), exiting.");
	}

}
