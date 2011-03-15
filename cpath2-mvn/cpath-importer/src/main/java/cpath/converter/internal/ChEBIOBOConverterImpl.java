package cpath.converter.internal;

// imports
import cpath.config.CPathSettings;
import cpath.dao.PaxtoolsDAO;

import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.controller.SimpleMerger;
import org.biopax.paxtools.io.simpleIO.SimpleEditorMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converter for ChEBI-OBO data.
 */
public class ChEBIOBOConverterImpl {

	// logger
    private static Log log = LogFactory.getLog(ChEBIOBOConverterImpl.class);
    
    //
	// chebi obo statics
	//
	private static final Pattern CHEBI_OBO_ID_REGEX = Pattern.compile("^id: CHEBI:(\\w+)$");
	private static final Pattern CHEBI_OBO_ISA_REGEX = Pattern.compile("^is_a: CHEBI:(\\w+)$");
	private static final Pattern CHEBI_OBO_RELATIONSHIP_REGEX = Pattern.compile("^relationship: (\\w+) CHEBI:(\\w+)$");
	private static final String REGEX_GROUP_DELIMITER = ":";
    
	private static final SimpleMerger MERGER = 
        new SimpleMerger(new SimpleEditorMap(BioPAXLevel.L3));
	
	// ref to warehouse dao/model - used to grab SMR's
	private Model model;
	private BioPAXFactory factory;
		
	/**
	 * Constructor.
	 * @param model Model
	 * @param factory BioPAXFactory
	 */
	public ChEBIOBOConverterImpl(Model model, BioPAXFactory factory) {
		this.model = model;
		this.factory = factory;
	}
	
	/** 
	 * At the time this class is constructed by
	 * BaseSDFConverter, the model it passes to constructor
	 * may be null.  This can be called to ensure model is valid.
	 * @param model
	 */
	public void setModel(final Model model) {
		this.model = model;
	}
	public Model getModel() { return model; }
	
	/**
	 * Given a ChEBI - OBO entry, creates proper member entity reference
	 * between parent and child.
	 * 
	 * @param entryBuffer
	 * @throws IOException
	 */
	public void processOBOEntry(StringBuffer entryBuffer) throws IOException {
	
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
			mergeSMR(parentSMR);
		}

		// we can also grab relationship (horizontal hierarchical info) 
		Collection<String> relationships = getValuesByREGEX(entryBuffer, CHEBI_OBO_RELATIONSHIP_REGEX);
		for (String relationship : relationships) {
			String[] parts = relationship.split(REGEX_GROUP_DELIMITER);
			RelationshipXref xref = getRelationshipXref(parts[0], parts[1]); 
			childSMR.addXref(xref);
		}
		
		// merge child back into model
		mergeSMR(childSMR);
	}
	
	/**
	 * Given an OBO entry, returns the values matched by the given regex.
	 * If regex contains more that one capture group, a ":" will be used to delimit them.
	 *  
	 * @param entryBuffer StringBuffer
	 * @param regex Pattern
	 * @return String
	 * @throws IOException
	 */
	private Collection<String> getValuesByREGEX(StringBuffer entryBuffer, Pattern regex) throws IOException {
		
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
		
		String rdfID = "urn:miriam:chebi:" + chebiID;
		return getByID(rdfID, SmallMoleculeReference.class);
	}
	
	/**
	 * Given a relationship from a ChEBI OBO file, returns a relationship xref.
	 * 
	 * @param relationshipType String
	 * @param chebiID String
	 */
	private RelationshipXref getRelationshipXref(String relationshipType, String chebiID) {
		
		RelationshipXref toReturn = null;
		
		// TODO: is this the best way to do this?
		// we use relationship type in rdf id of xref since there can be many to many relation types
		// bet SM.  For example CHEBI:X has_part CHEBI:Y and CHEBI:Z is_conjugate_acid_of CHEBI:Y
		// we need distinct rxref to has_part CHEBI:Y and is_conjugate_acid_of CHEBI:Y
		String xrefRdfID = CPathSettings.CPATH_URI_PREFIX + RelationshipXref.class.getSimpleName() + ":" + 
			URLEncoder.encode(relationshipType.toUpperCase() + "_CHEBI_" + chebiID);
		
		if (model.containsID(xrefRdfID)) {
			return getByID(xrefRdfID, RelationshipXref.class);
		}
		
		// made it here, need to create relationship xref
		toReturn = (RelationshipXref)factory.reflectivelyCreate(RelationshipXref.class);
		toReturn.setRDFId(xrefRdfID);
		toReturn.setDb("CHEBI");
		toReturn.setId(chebiID);
		
		// set relationship type vocabulary on the relationship xref
		String relTypeRdfID =
			CPathSettings.CPATH_URI_PREFIX + RelationshipTypeVocabulary.class.getSimpleName() +
			":" + URLEncoder.encode("CHEBI_" + relationshipType.toUpperCase());
		
		if (model.containsID(relTypeRdfID)) {
			toReturn.setRelationshipType(getByID(relTypeRdfID, RelationshipTypeVocabulary.class));
		}
		else {
			RelationshipTypeVocabulary rtv = model.addNew(RelationshipTypeVocabulary.class, relTypeRdfID);
			rtv.addTerm(relationshipType);
			toReturn.setRelationshipType(rtv);
		}
		
		// outta here
		return toReturn;
	}

	/**
	 * Given an RDF ID and class type, returns an instance of the class
	 * from the warehouse.
	 * 
	 * @param <T>
	 * @param urn
	 * @param type
	 * @return
	 */
	private <T extends BioPAXElement> T getByID(String urn, Class<T> type) {

		T bpe = (T) model.getByID(urn);
		if (bpe != null && model instanceof PaxtoolsDAO) {
			// initialize before finally detaching it
			((PaxtoolsDAO) model).initialize(bpe);
		}
		
		// outta here
		return bpe;
		
    }
	
	/**
	 * Merges the given SMR back into the model
	 * 
	 * @param smr SmallMoleculeReference
	 */
	private void mergeSMR(SmallMoleculeReference smr) {
		
		if (model instanceof PaxtoolsDAO) {
        	((PaxtoolsDAO)model).merge(smr);
        }
        else {
        	// make a self-consistent sub-model from the smr
        	MERGER.merge(model, smr);
        }
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