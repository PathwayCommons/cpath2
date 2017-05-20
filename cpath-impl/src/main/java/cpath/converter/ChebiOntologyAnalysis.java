package cpath.converter;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cpath.service.CPathUtils;
import cpath.service.RelTypeVocab;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpath.service.Analysis;


/**
 * Using the ChEBI ontology (OBO), adds intra-ChEBI ontology relationships to the warehouse BioPAX model.
 * This should run after the ChEBI OBO data were first converted to the small molecule warehouse BioPAX model.
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
	 * Given a ChEBI OBO entry, adds parent-child ('is_a') and other relations,
	 * such as 'has_part', 'has_role', etc. (creates BioPAX relationship xrefs)
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

		// link each parent ChEBI to the child (i.e., child 'is_a' parent relation)
		Collection<String> parentChebiIDs = getValuesByREGEX(entryBuffer, CHEBI_OBO_ISA_REGEX);
		for (String parentChebiID : parentChebiIDs) {
			RelationshipXref xref = CPathUtils.findOrCreateRelationshipXref(RelTypeVocab.MULTIPLE_PARENT_REFERENCE,
						"chebi", "CHEBI:"+parentChebiID, model);
			thisSMR.addComment("is_a CHEBI:" + parentChebiID);
			thisSMR.addXref(xref);
		}

		// store horizontal relationships between ChEBI terms (has_part, has_role, is_conjugate_*,..)
		Collection<String> relationships = getValuesByREGEX(entryBuffer, CHEBI_OBO_RELATIONSHIP_REGEX);
		for (String relationship : relationships) {
			String[] parts = relationship.split(_COLON);
			RelationshipXref xref = CPathUtils.findOrCreateRelationshipXref(RelTypeVocab.ADDITIONAL_INFORMATION,
						"chebi", "CHEBI:"+parts[1], model);
			thisSMR.addComment(parts[0].toLowerCase() + " CHEBI:" + parts[1]);
			thisSMR.addXref(xref);
		}
	}

	/**
	 * Given an OBO entry, returns the values matched by the given regex. If
	 * regex contains more that one capture group, a ":" will be used to delimit
	 * them.
	 */
	private Collection<String> getValuesByREGEX(StringBuilder entryBuffer,
			Pattern regex) throws IOException {

		Collection<String> toReturn = new ArrayList<String>();
		Scanner scanner = new Scanner(entryBuffer.toString());

		if (log.isDebugEnabled()) {
			log.debug("getValue(), key: " + regex.toString());
		}

		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			Matcher matcher = regex.matcher(line);
			if (matcher.find()) {
				String toAdd = "";
				for (int lc = 1; lc <= matcher.groupCount(); lc++) {
					toAdd += matcher.group(lc) + _COLON;
				}
				toReturn.add(toAdd.substring(0, toAdd.length() - 1));//to remove ending ':'
			}
		}

		return toReturn;
	}


	public void execute(Model model) {
		log.info("convert(), starting to read data...");
		
		if(inputStream == null)
			throw new IllegalArgumentException("The second parameter must be not null input stream");
		
		try {
			Scanner scanner = new Scanner(inputStream);

			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				// start of entry
				if (line.startsWith(CHEBI_OBO_ENTRY_START)) {
					StringBuilder entryBuffer = new StringBuilder(line + "\n");
					while (scanner.hasNextLine()) {
						line = scanner.nextLine();
						// keep reading until we reach last modified
						if (line.isEmpty())
							break;
						entryBuffer.append(line + "\n");
					}

					processOBOEntry(entryBuffer, model);
				}
			}

			scanner.close();

		} catch (IOException e) {
			log.error("is.close() failed." + e);
		}
		
		log.info("convert(), exiting.");
	}

}
