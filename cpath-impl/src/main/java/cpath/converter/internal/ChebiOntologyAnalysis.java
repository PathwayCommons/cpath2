package cpath.converter.internal;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpath.importer.PreMerger;
import cpath.importer.PreMerger.RelTypeVocab;
import cpath.service.Analysis;


/**
 * Using the ChEBI ontology (OBO),
 * adds intra-ChEBI relationships to the BioPAX model.
 * 
 * This can go after the small molecule warehouse 
 * is built from ChEBI data (or even after cPath2 Merge stage). 
 */
final class ChebiOntologyAnalysis implements Analysis<Model>
{
	private InputStream inputStream; //ChEBI OBO data
	
	public void setInputStream(InputStream is) {
		this.inputStream = is;
	}
	
	private static final String CHEBI_OBO_ENTRY_START = "[Term]";	
    private static final Logger log = LoggerFactory.getLogger(ChebiOntologyAnalysis.class);
	  
	private final Pattern CHEBI_OBO_ID_REGEX = Pattern.compile("^id: CHEBI:(\\w+)$");
	private final Pattern CHEBI_OBO_ISA_REGEX = Pattern.compile("^is_a: CHEBI:(\\w+)$");
	private final Pattern CHEBI_OBO_RELATIONSHIP_REGEX = Pattern.compile("^relationship: (\\w+) CHEBI:(\\w+)$");
	private final String _COLON = ":";
	
	
	/**
	 * Given a ChEBI - OBO entry, creates memberEntityReference link 
	 * between the parent and child, and adds ChEBI internal relationship xrefs.
	 * 
	 * @param entryBuffer
	 * @param model existing biopax Warehouse model
	 * @throws IOException
	 */
	private void processOBOEntry(StringBuilder entryBuffer, Model model) throws IOException {
		if (log.isDebugEnabled()) {
			log.debug("calling processOBOEntry()");
		}
		// get SMR for entry out of Warehouse
		Collection<String> childChebiIDs = getValuesByREGEX(entryBuffer, CHEBI_OBO_ID_REGEX);
		if (childChebiIDs.size() != 1) {
			log.error("processOBOEntry(), got none or >1 ID in: " + entryBuffer.toString() + "; skipped.");
			return;
		}
		final String thisID = childChebiIDs.iterator().next();
		SmallMoleculeReference thisSMR = (SmallMoleculeReference) model
			.getByID("http://identifiers.org/chebi/CHEBI:" + thisID);
		if (thisSMR == null) {
			log.debug("processOBOEntry(), Skipped (not found): " + thisID);
			return;
		}

		// for each parent ChEBI, create a member entity reference to child
		Collection<String> parentChebiIDs = getValuesByREGEX(entryBuffer, CHEBI_OBO_ISA_REGEX);
		for (String parentChebiID : parentChebiIDs) {
			SmallMoleculeReference parentSMR = (SmallMoleculeReference) model
				.getByID("http://identifiers.org/chebi/CHEBI:" + parentChebiID);
			
			//always add a rel. xref and is_a comments:
			log.info("processOBOEntry(), CHEBI:" + thisID 
					+ " 'IS_A' already ignored CHEBI:" + parentChebiID 
					+ " (generic, no InChIKey); will add xref/comment instead of memberEntityRef.");
			RelationshipXref xref = PreMerger
				.findOrCreateRelationshipXref(RelTypeVocab.MULTIPLE_PARENT_REFERENCE, 
						"CHEBI", "CHEBI:"+parentChebiID, model);
			thisSMR.addComment("is_a CHEBI:" + parentChebiID);
			thisSMR.addXref(xref);
			
			//if the parent SMR exists in the model (has InChIKey) -
			if (parentSMR != null) {
				parentSMR.addMemberEntityReference(thisSMR);
			}
		}

		// store horizontal relationships between ChEBI terms (has_part, has_role, is_conjugate_*,..)
		Collection<String> relationships = getValuesByREGEX(entryBuffer, CHEBI_OBO_RELATIONSHIP_REGEX);
		for (String relationship : relationships) {
			String[] parts = relationship.split(_COLON);
			RelationshipXref xref = PreMerger
				.findOrCreateRelationshipXref(RelTypeVocab.ADDITIONAL_INFORMATION, 
						"ChEBI", "CHEBI:"+parts[1], model);		
			thisSMR.addComment(parts[0].toLowerCase() + " CHEBI:" + parts[1]);
			thisSMR.addXref(xref);
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
					toAdd += matcher.group(lc) + _COLON;
				}
				toReturn.add(toAdd.substring(0, toAdd.length() - 1));//to remove ending ':'
			}
			line = reader.readLine();
		}

		return toReturn;
	}


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
					while ((line = reader.readLine()) != null) {
						// keep reading until we reach last modified
						if (line.isEmpty())
							break;
						entryBuffer.append(line + "\n");
					}

					processOBOEntry(entryBuffer, model);
				}
			}

			if (reader != null)
				reader.close();
		} catch (IOException e) {
			log.error("is.close() failed." + e);
		}
		
		log.info("convert(), exiting.");
	}

}
