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

package cpath.warehouse.internal;

//import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.miriam.MiriamLink;
import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.level3.ControlledVocabulary;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.proxy.level3.BioPAXFactoryForPersistence;
import org.biopax.validator.utils.BiopaxOntologyManager;
import org.springframework.core.io.Resource;

import psidev.ontology_manager.Ontology;
import psidev.ontology_manager.OntologyTermI;

import cpath.warehouse.CvRepository;



/**
 * This is to access OBO Cvs:
 * 
 * @author rodch
 *
 */
public final class OntologyManagerCvRepository extends BiopaxOntologyManager implements CvRepository {
	private static final Log log = LogFactory.getLog(OntologyManagerCvRepository.class);
	private static final String URN_OBO_PREFIX = "urn:miriam:obo.";
	private static BioPAXFactory biopaxFactory = new BioPAXFactoryForPersistence();
	
	/**
	 * Constructor
	 * 
	 * @param ontologies ontology config XML resource (for OntologyManager)
	 * @param miriam
	 * @param ontTmpDir TODO
	 * @throws Exception
	 */
	public OntologyManagerCvRepository(Resource ontologies, String ontTmpDir) {
		super(ontologies, ontTmpDir);
		
		/* Normalize (for safety :)) ontology names using ID naming convention in ontologies.xml
		 * This is also a good check that everything's ok...
		 */
		for(String id : getOntologyIDs()) {
			String officialName = MiriamLink.getName(id); 
			Ontology o = getOntology(id);
			o.setName(officialName);
		}
	}
	

	/* (non-Javadoc)
	 * @see cpath.warehouse.CvRepository#getByDbAndId(java.lang.String, java.lang.String, java.lang.Class)
	 */
	@Override
	public <T extends ControlledVocabulary> T getControlledVocabulary(String db,
			String id, Class<T> cvClass) {
		// TODO Auto-generated method stub
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
		
		T cv = biopaxFactory.reflectivelyCreate(cvClass);
		cv.setRDFId(urn);
		cv.addTerm(term.getPreferredName());
		
		UnificationXref uref = biopaxFactory.reflectivelyCreate(UnificationXref.class);
		String ontId = term.getOntologyId(); // like "GO" 
		String db = getOntology(ontId).getName(); // names were fixed in the constructor!
		uref.setDb(db); 
		String rdfid = "http://biopax.org/UnificationXref#" + 
			URLEncoder.encode(uref.getDb() + "_" + term.getTermAccession());
		uref.setRDFId(rdfid);
		uref.setId(term.getTermAccession());
		cv.addXref(uref);
		
		return cv;
	}
	
	
	public Set<String> getDirectChildren(String urn) {
		OntologyTermI term = getTermByUrn(urn);
		Ontology ontology = getOntology(term.getOntologyId());
		Collection<OntologyTermI> terms = ontology.getDirectChildren(term);
		return ontologyTermsToUrns(terms);
	}

	
	public Set<String> getDirectParents(String urn) {
		OntologyTermI term = getTermByUrn(urn);
		Ontology ontology = getOntology(term.getOntologyId());
		Collection<OntologyTermI> terms = ontology.getDirectParents(term);
		return ontologyTermsToUrns(terms);
	}

	
	public Set<String> getAllChildren(String urn) {
		OntologyTermI term = getTermByUrn(urn);
		Ontology ontology = getOntology(term.getOntologyId());
		Collection<OntologyTermI> terms = ontology.getAllChildren(term);
		return ontologyTermsToUrns(terms);
	}

	
	public Set<String> getAllParents(String urn) {
		OntologyTermI term = getTermByUrn(urn);
		Ontology ontology = getOntology(term.getOntologyId());
		Collection<OntologyTermI> terms = ontology.getAllParents(term);
		return ontologyTermsToUrns(terms);
	}
	
	
	public boolean isChild(String parentUrn, String urn) {
		return getAllParents(urn).contains(parentUrn)
			|| getAllChildren(parentUrn).contains(urn);
	}
	
	
	/* ==========================================================================*
	 *        Internal Methods (package-private - for easy testing)              *
	 * ==========================================================================*/
	

	OntologyTermI getTermByUrn(String urn) {
		if(urn.startsWith(URN_OBO_PREFIX)) {
			int l = URN_OBO_PREFIX.length();
			int indexOfTheColon = urn.indexOf(':', l);
			String acc = urn.substring(indexOfTheColon+1);
			acc=URLDecoder.decode(acc);
			String dtUrn = urn.substring(0, indexOfTheColon);				
			OntologyTermI term = findTermByAccession(acc); // acc. is globally unique in OntologyManager!..
			return term;
		} else {
			log.error("Cannot Decode not a Controlled Vocabulary's URI : " + urn);
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
	
	
	Set<String> ontologyTermsToUrns(Collection<OntologyTermI> terms) {
		Set<String> urns = new HashSet<String>();
		
		for(OntologyTermI term : terms) {
			String ontologyID = term.getOntologyId();
			String accession = term.getTermAccession();
			String urn = MiriamLink.getURI(ontologyID, accession);
			urns.add(urn);
		}
		
		return urns;
	}
	
	
/*
 * TODO Methods below would guarantee working only with BioPAX-recommended CVs...
 */

/*
	public Set<Cv> fetchBiopaxCVs(Level3CvTermsRule cvRule) {
		Set<Cv> beans = new HashSet<Cv>();
		
		// find the CV class name
		String cvClassName = (cvRule.getEditor() == null) 
			? cvRule.getDomain().getSimpleName()
			: cvRule.getEditor().getRange().getSimpleName();
		
		Set<OntologyTermI> terms = getValidTerms(cvRule);
		// create Cv beans hierarchy (recursively)
		for(OntologyTermI term : terms) {
			Cv bean = createBean(beans, terms, term, cvClassName);
			beans.add(bean);
		}
		
		return beans;
	}


	private Cv createBean(final Set<Cv> beans, final Set<OntologyTermI> terms, final OntologyTermI term, final String type) {
		// get allowed terms
		String ontology = term.getOntologyName();
		String accession = term.getTermAccession();
		String urn = miriam.getURI(ontology, accession);
		Cv bean = new Cv(type, urn, ontology, accession, term.getPreferredName());
		
		if (!beans.contains(bean)) {
			bean.getSynonyms().addAll(term.getNameSynonyms());
			for (OntologyTermI childTerm : getOntologyAccess(ontology).getDirectChildren(term)) 
			{
				// sure, we use only valid children terms (remember the CV Rule)
				if (terms.contains(childTerm)) {
					Cv childBean = createBean(beans, terms, childTerm, type);
					bean.addMember(childBean);
					beans.add(childBean);
				}
			}
		}
		
		return bean;
	}
*/
	
}
