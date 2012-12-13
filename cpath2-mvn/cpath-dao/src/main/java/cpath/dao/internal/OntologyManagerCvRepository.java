/**
 ** Copyright (c) 2009 Memorial Sloan-Kettering Cancer Center (MSKCC)
 ** and University of Toronto (UofT).
 **
 ** This is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** any later version.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** both UofT and MSKCC have no obligations to provide maintenance, 
 ** support, updates, enhancements or modifications.  In no event shall
 ** UofT or MSKCC be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** UofT or MSKCC have been advised of the possibility of such damage.  
 ** See the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this software; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA;
 ** or find it at http://www.fsf.org/ or http://www.gnu.org.
 **/

package cpath.dao.internal;

import java.net.URLDecoder;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.miriam.MiriamLink;
import org.biopax.paxtools.controller.Fetcher;
import org.biopax.paxtools.controller.SimpleEditorMap;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.ControlledVocabulary;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.model.level3.XReferrable;
import org.biopax.paxtools.model.level3.Xref;
import org.biopax.psidev.ontology_manager.Ontology;
import org.biopax.psidev.ontology_manager.OntologyTermI;
import org.biopax.validator.utils.BiopaxOntologyManager;
import org.biopax.validator.utils.Normalizer;

import cpath.config.CPathSettings;
import cpath.config.CPathSettings.CPath2Property;
import cpath.warehouse.CvRepository;
import cpath.warehouse.WarehouseDAO;



/**
 * This is to access OBO Cvs:
 * 
 * @author rodche
 *
 */
class OntologyManagerCvRepository extends BiopaxOntologyManager 
	implements CvRepository, WarehouseDAO 
{
	private static final Log log = LogFactory.getLog(OntologyManagerCvRepository.class);
	private static final String URN_OBO_PREFIX = "urn:miriam:obo.";
	private static final String URL_OBO_PREFIX = "http://identifiers.org/obo.";
	private static BioPAXFactory biopaxFactory = BioPAXLevel.L3.getDefaultFactory();
	private static final Fetcher FETCHER = new Fetcher(SimpleEditorMap.L3);
	
	/**
	 * Constructor
	 * 
	 * @param ontologies ontology config XML resource (for OntologyManager)
	 * @param miriam
	 * @param ontTmpDir
	 * @throws Exception
	 */
	public OntologyManagerCvRepository(Properties ontologies, 
			String ontTmpDir, boolean isReuseAndStoreOBOLocally) 
	{
		super(ontologies, ontTmpDir, isReuseAndStoreOBOLocally);
		
		//Normalize (for safety :)) ontology names using IDs
		for(String id : getOntologyIDs()) {
			String officialName = MiriamLink.getName(id);
			Ontology o = getOntology(id);
			o.setName(officialName);
			
			if(log.isDebugEnabled())
				log.debug(id + " (" + officialName + ") from " + ontologies.get(id) 
					+ " (store/use_local_file=" + isReuseAndStoreOBOLocally + ")");
		}
	}
	

	/* (non-Javadoc)
	 * @see cpath.warehouse.CvRepository#getByDbAndId(java.lang.String, java.lang.String, java.lang.Class)
	 */
	@Override
	public <T extends ControlledVocabulary> T getControlledVocabulary(
			String db, String id, Class<T> cvClass) 
	{
		OntologyTermI term = null;
		
		Ontology ontology = getOntology(db);
		if(ontology == null) // it may be urn -
			ontology = getOntologyByUrn(db);
		
		if (ontology != null) {
			term = ontology.getTermForAccession(id);
		} else { // still null? well, no problem -
			/*
			 * surprisingly or by design, "accession" is a unique key (through
			 * all ontologies) in the ontology manager
			 */
			term = findTermByAccession(id);
		}
		
		if(term != null) {
			return getControlledVocabulary(term, cvClass);
		} 
		
		return null;
	}


	/* (non-Javadoc)
	 * @see cpath.warehouse.CvRepository#getById(java.lang.String, java.lang.Class)
	 */
	@Override
	public <T extends ControlledVocabulary> T getControlledVocabulary(String urn,
			Class<T> cvClass) 
	{
		OntologyTermI term = getTermByUrn(urn);
		T cv = getControlledVocabulary(term, cvClass);
		if(cv != null)
			cv.addComment(CPathSettings.CPATH2_GENERATED_COMMENT);
		return cv;
	}
	
	
	public Set<String> getDirectChildren(String urn) {
		OntologyTermI term = getTermByUrn(urn);
		Ontology ontology = getOntology(term.getOntologyId());
		Collection<OntologyTermI> terms = ontology.getDirectChildren(term);
		return ontologyTermsToUris(terms);
	}

	
	public Set<String> getDirectParents(String urn) {
		OntologyTermI term = getTermByUrn(urn);
		Ontology ontology = getOntology(term.getOntologyId());
		Collection<OntologyTermI> terms = ontology.getDirectParents(term);
		return ontologyTermsToUris(terms);
	}

	
	public Set<String> getAllChildren(String urn) {
		OntologyTermI term = getTermByUrn(urn);
		Ontology ontology = getOntology(term.getOntologyId());
		Collection<OntologyTermI> terms = ontology.getAllChildren(term);
		return ontologyTermsToUris(terms);
	}

	
	public Set<String> getAllParents(String urn) {
		OntologyTermI term = getTermByUrn(urn);
		Ontology ontology = getOntology(term.getOntologyId());
		Collection<OntologyTermI> terms = ontology.getAllParents(term);
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
		String rdfid = Normalizer.uri(CPathSettings.get(CPath2Property.XML_BASE), db, term.getTermAccession(), UnificationXref.class);
		UnificationXref uref = biopaxFactory.create(UnificationXref.class, rdfid);
		uref.setDb(db); 
		uref.setId(term.getTermAccession());
		cv.addXref(uref);
		
		return cv;
	}
	

	OntologyTermI getTermByUrn(String urn) {
		if(urn.startsWith(URN_OBO_PREFIX)) {
			int pos = urn.indexOf(':', URN_OBO_PREFIX.length()); //e.g. the colon after 'go' in "...:obo.go:GO%3A0005654"
			String acc = urn.substring(pos+1);
			acc=URLDecoder.decode(acc);
//			String dtUrn = urn.substring(0, pos);				
			OntologyTermI term = findTermByAccession(acc); // acc. is globally unique in OntologyManager!..
			return term;
		} else if(urn.startsWith(URL_OBO_PREFIX)) {
			int pos = urn.indexOf('/', URL_OBO_PREFIX.length()); //e.g. the slash after 'go' in ".../obo.go/GO:0005654"
			String acc = urn.substring(pos+1);				
			OntologyTermI term = findTermByAccession(acc); // acc. is globally unique in OntologyManager!..
			return term;
		} else {
			if(log.isDebugEnabled())
				log.debug("Cannot Decode not a Controlled Vocabulary's URI : " + urn);
			return null;
		}
	}
	
	
	/*
	 * Gets Ontology by (Miriam's) datatype URI
	 */
	Ontology getOntologyByUrn(String dtUrn) {
		for (String id : getOntologyIDs()) {
			Ontology ont = getOntology(id);
			String urn = MiriamLink.getDataTypeURI(id);
			if(dtUrn.equalsIgnoreCase(urn)) {
				return ont;
			}
		}
		return null;
	}
	
	
	Set<String> ontologyTermsToUris(Collection<OntologyTermI> terms) {
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


	/* (non-Javadoc)
	 * @see cpath.dao.WarehouseDAO#getObject(java.lang.String, java.lang.Class)
	 */
	// TODO validate if the ontology term (form URN) can be used by this CV class?
	@Override
	public <T extends BioPAXElement> T createBiopaxObject(String urn, Class<T> clazz) {
		return (T) getControlledVocabulary(urn,(Class<ControlledVocabulary>) clazz);
	}

	
	@Override
	public Set<String> findByXref(Set<? extends Xref> xrefs, Class<? extends XReferrable> clazz)
	{
		Set<String> toReturn = new HashSet<String>();
		
		for (Xref xref : xrefs) {
			ControlledVocabulary cv = getControlledVocabulary(xref.getDb(), xref.getId(),
					(Class<ControlledVocabulary>) clazz);
			
			// TODO validate if the ontology terms (form xrefs) can be used by this CV class?
			
			if (cv != null) {
				toReturn.add(cv.getRDFId());
			}
		}

		return toReturn;
	}


	@Override
	public <T extends BioPAXElement> Model createSubModel(String urn,
			Class<T> clazz) {
		
		T t = createBiopaxObject(urn, clazz);
		if(t == null)
			return null;
		
		Model m = biopaxFactory.createModel();
		FETCHER.fetch(t, m);
		return m;
		
	}
	
}
