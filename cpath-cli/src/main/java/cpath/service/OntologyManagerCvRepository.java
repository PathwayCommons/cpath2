package cpath.service;

import java.net.URLDecoder;
import java.util.*;

import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.level3.ControlledVocabulary;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.normalizer.MiriamLink;
import org.biopax.paxtools.normalizer.Normalizer;
import org.biopax.psidev.ontology_manager.OntologyAccess;
import org.biopax.psidev.ontology_manager.OntologyTermI;
import org.biopax.validator.utils.BiopaxOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpath.config.CPathSettings;

/**
 * This is to access OBO Cvs:
 * 
 * @author rodche
 *
 */
public class OntologyManagerCvRepository extends BiopaxOntologyManager 
	implements CvRepository 
{
	private static final Logger log = LoggerFactory.getLogger(OntologyManagerCvRepository.class);
	private static BioPAXFactory biopaxFactory = BioPAXLevel.L3.getDefaultFactory();
	
	/**
	 * Constructor
	 * 
	 * @param ontologies ontology config XML resource (for OntologyManager)
	 * @throws Exception
	 */
	public OntologyManagerCvRepository(Properties ontologies) 
	{
		super(ontologies);
		
		//Normalize (for safety :)) ontology names using IDs
		for(String id : getOntologyIDs()) {
			String officialName = MiriamLink.getName(id);
			OntologyAccess o = getOntology(id);
			o.setName(officialName);
			log.debug(id + " (" + officialName + ") from " + ontologies.get(id));
		}
	}

	
	@Override
	public <T extends ControlledVocabulary> T getControlledVocabulary(
			String db, String id, Class<T> cvClass) 
	{
		OntologyTermI term = null;
		
		OntologyAccess ontologyAccess = getOntology(db);
		if(ontologyAccess == null) // it may be urn -
			ontologyAccess = getOntologyByUrn(db);
		
		term = findTerm(ontologyAccess, id);
		
		if(term != null) {
			return getControlledVocabulary(term, cvClass);
		}
		
		return null;
	}

	
	private OntologyTermI findTerm(OntologyAccess ontologyAccess, String term) {
		
		if(ontologyAccess == null) {
			return findTermByAccession(term); 
			//done
		}
		
		//otherwise continue with more specific lookup			
		OntologyTermI ot = ontologyAccess.getTermForAccession(term);
		
		if(ot == null) {
			//search again using the parameter as term's name/synonym
			Set<OntologyTermI> ots = searchTermByName(term, 
					Collections.singleton(ontologyAccess.getName()));		
			if(ots.size() == 1) //use if unambiguous 
				ot = ots.iterator().next();
			else 
				log.info("ambiguous term: " + term + 
					" found by searchig in ontology: " + ontologyAccess.getName());
		}
		
		return ot;
	}



	/* (non-Javadoc)
	 * @see cpath.warehouse.CvRepository#getById(java.lang.String, java.lang.Class)
	 */
	@Override
	public <T extends ControlledVocabulary> T getControlledVocabulary(String urn,
			Class<T> cvClass) 
	{
		OntologyTermI term = getTermByUri(urn);
		T cv = getControlledVocabulary(term, cvClass);
		if(cv != null)
			cv.addComment(CPathSettings.CPATH2_GENERATED_COMMENT);
		return cv;
	}
	
	
	public Set<String> getDirectChildren(String urn) {
		OntologyTermI term = getTermByUri(urn);
		OntologyAccess ontologyAccess = getOntology(term.getOntologyId());
		Collection<OntologyTermI> terms = ontologyAccess.getDirectChildren(term);
		return ontologyTermsToUris(terms);
	}

	
	public Set<String> getDirectParents(String urn) {
		OntologyTermI term = getTermByUri(urn);
		OntologyAccess ontologyAccess = getOntology(term.getOntologyId());
		Collection<OntologyTermI> terms = ontologyAccess.getDirectParents(term);
		return ontologyTermsToUris(terms);
	}

	
	public Set<String> getAllChildren(String urn) {
		OntologyTermI term = getTermByUri(urn);
		OntologyAccess ontologyAccess = getOntology(term.getOntologyId());
		Collection<OntologyTermI> terms = ontologyAccess.getAllChildren(term);
		return ontologyTermsToUris(terms);
	}

	
	public Set<String> getAllParents(String urn) {
		OntologyTermI term = getTermByUri(urn);
		OntologyAccess ontologyAccess = getOntology(term.getOntologyId());
		Collection<OntologyTermI> terms = ontologyAccess.getAllParents(term);
		return ontologyTermsToUris(terms);
	}
	
	
	public boolean isChild(String parentUrn, String urn) {
		return getAllParents(urn).contains(parentUrn)
			|| getAllChildren(parentUrn).contains(urn);
	}
	
	
	/* ==========================================================================*
	 *        Internal Methods (package-private - for easy testing)              *
	 * ==========================================================================*/
	
	
	<T extends ControlledVocabulary> T getControlledVocabulary(OntologyTermI term,
			Class<T> cvClass) 
	{
		if(term == null)
			return null;
		
		String urn = ontologyTermToUri(term);
		T cv = biopaxFactory.create(cvClass, urn);
		cv.addTerm(term.getPreferredName());
		
		String ontId = term.getOntologyId(); // like "GO" 
		String db = getOntology(ontId).getName(); // names were fixed in the constructor!
		String rdfid = Normalizer.uri(CPathSettings.getInstance().getXmlBase(), db, term.getTermAccession(), UnificationXref.class);
		UnificationXref uref = biopaxFactory.create(UnificationXref.class, rdfid);
		uref.setDb(db); 
		uref.setId(term.getTermAccession());
		cv.addXref(uref);
		
		return cv;
	}
	

	/*
	 * 	Some CV URI/URLs can include 
	 *  'obo.' in it (now deprecated) or not, like e.g.
	 *  'obo.so', 'obo.go' vs. simply 'so', 'go'
	 */
	OntologyTermI getTermByUri(String uri) {
		if(uri.startsWith("urn:miriam:obo.")) {
			int pos = uri.indexOf(':', 15); //e.g. the colon after 'go' in "...:obo.go:GO%3A0005654"
			String acc = uri.substring(pos+1);
			acc=URLDecoder.decode(acc);
			OntologyTermI term = findTermByAccession(acc); // acc. is globally unique in OntologyManager!..
			return term;
		} else if(uri.startsWith("http://identifiers.org/obo.")) {
			int pos = uri.indexOf('/', 27); //e.g. the slash after 'go' in "...obo.go/GO:0005654"
			String acc = uri.substring(pos+1);				
			OntologyTermI term = findTermByAccession(acc);
			return term;
		} else if(uri.startsWith("urn:miriam:")) {
			int pos = uri.indexOf(':', 11); //e.g. the last colon in "...:go:GO%3A0005654"
			String acc = uri.substring(pos+1);
			acc=URLDecoder.decode(acc);
			OntologyTermI term = findTermByAccession(acc);
			return term;
		} else if(uri.startsWith("http://identifiers.org/")) {
			int pos = uri.indexOf('/', 23); //e.g. the slash after 'org/go' in "...org/go/GO:0005654"
			String acc = uri.substring(pos+1);				
			OntologyTermI term = findTermByAccession(acc); 
			return term;
		} else {
			if(log.isDebugEnabled())
				log.debug("Cannot Decode not a Controlled Vocabulary's URI : " + uri);
			return null;
		}
	}
	
	
	/*
	 * Gets OntologyAccess by (Miriam's) datatype URI
	 */
	OntologyAccess getOntologyByUrn(String dtUrn) {
		for (String id : getOntologyIDs()) {
			OntologyAccess ont = getOntology(id);
			String urn = MiriamLink.getDataTypeURI(id);
			if(dtUrn.equalsIgnoreCase(urn)) {
				return ont;
			}
		}
		return null;
	}
	
	
	public Set<String> ontologyTermsToUris(Collection<OntologyTermI> terms) {
		Set<String> urns = new HashSet<String>();
		for(OntologyTermI term : terms) {
			urns.add(ontologyTermToUri(term));
		}
		return urns;
	}
	
	
	String ontologyTermToUri(OntologyTermI term) {
		String uri = null;
		if (term != null) {
			String ontologyID = term.getOntologyId();
			String accession = term.getTermAccession();
			uri = MiriamLink.getIdentifiersOrgURI(ontologyID, accession);
		}
		return uri;
	}
	
}
