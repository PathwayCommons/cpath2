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

package cpath.fetcher.internal;

//import java.io.IOException;
import java.io.File;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.miriam.MiriamLink;
import org.biopax.validator.utils.OntologyManagerAdapter;
import org.springframework.core.io.Resource;

//import psidev.psi.tools.ontology_manager.OntologyManagerContext;
import psidev.psi.tools.ontology_manager.OntologyManagerContext;
import psidev.psi.tools.ontology_manager.impl.OntologyTermImpl;
import psidev.psi.tools.ontology_manager.interfaces.OntologyAccess;
import psidev.psi.tools.ontology_manager.interfaces.OntologyTermI;

import cpath.warehouse.CvRepository;
import cpath.warehouse.beans.Cv;



/**
 * This is to access OBO Cvs:
 * 
 * @author rodch
 *
 */
public final class CvFetcher extends OntologyManagerAdapter implements CvRepository {
	private final static Log log = LogFactory.getLog(CvFetcher.class);
	
	private MiriamLink miriam;
	
	/**
	 * Constructor
	 * 
	 * @param ontologies ontology config XML resource (for OntologyManager)
	 * @param miriam
	 * @param ontTmpDir TODO
	 * @throws Exception
	 */
	public CvFetcher(Resource ontologies, MiriamLink miriam, String ontTmpDir) {
		super(ontologies);
		this.miriam = miriam;
		OntologyManagerContext.getInstance()
			.setOntologyDirectory(new File(ontTmpDir));
		if(log.isInfoEnabled())
			log.info("Using ontologies cache directory : " + ontTmpDir);
		
	}
	
	
	public Set<String> getDirectChildren(String urn) {
		OntologyTermI term = getTermByUrn(urn);
		OntologyAccess ontologyAccess = getOntologyAccess(term.getOntologyName());
		Collection<OntologyTermI> terms = ontologyAccess.getDirectChildren(term);
		return ontologyTermsToUrns(terms);
	}

	
	public Set<String> getDirectParents(String urn) {
		OntologyTermI term = getTermByUrn(urn);
		OntologyAccess ontologyAccess = getOntologyAccess(term.getOntologyName());
		Collection<OntologyTermI> terms = ontologyAccess.getDirectParents(term);
		return ontologyTermsToUrns(terms);
	}

	
	public Set<String> getAllChildren(String urn) {
		OntologyTermI term = getTermByUrn(urn);
		OntologyAccess ontologyAccess = getOntologyAccess(term.getOntologyName());
		Collection<OntologyTermI> terms = ontologyAccess.getAllChildren(term);
		return ontologyTermsToUrns(terms);
	}

	
	public Set<String> getAllParents(String urn) {
		OntologyTermI term = getTermByUrn(urn);
		OntologyAccess ontologyAccess = getOntologyAccess(term.getOntologyName());
		Collection<OntologyTermI> terms = ontologyAccess.getAllParents(term);
		return ontologyTermsToUrns(terms);
	}
	
	
	OntologyTermI searchForTermByAccession(String acc) {
		OntologyTermI term = null;
		
		for(String ontologyId : getOntologyIDs()) {
			term = getOntologyAccess(ontologyId).getTermForAccession(acc);
			if(term != null) 
				break;
		}
		
		return term;
	}
	
	
	
	/* CvRepository interface implementation */

	/* (non-Javadoc)
	 * @see cpath.warehouse.CvRepository#add(cpath.warehouse.beans.Cv)
	 */
	public void add(Cv cv) {
		throw new UnsupportedOperationException("'add' " +
				"operation is not supported in this implementation!");
	}


	/* (non-Javadoc)
	 * @see cpath.warehouse.CvRepository#getById(java.lang.String)
	 */
	public Cv getById(String urn) {
		Cv cv = new Cv(urn); // parse urn

		OntologyTermI term = getTermByUrn(urn);
		// set children
		cv.setChildren(getDirectChildren(urn));
		// set parents
		cv.setParents(getDirectParents(urn));
		// add names
		cv.addName(term.getPreferredName());
		cv.getNames().addAll(term.getNameSynonyms());

		return cv;
	}


	/* (non-Javadoc)
	 * @see cpath.warehouse.CvRepository#isChild(java.lang.String, java.lang.String)
	 */
	public boolean isChild(String parentUrn, String urn) {
		return getAllParents(urn).contains(parentUrn)
			|| getAllChildren(parentUrn).contains(urn);
	}
	
	
	
	
	OntologyTermI getTermByUrn(String urn) {
		Cv cv = new Cv(urn); // parse urn
		OntologyTermI term = new OntologyTermImpl(cv.getAccession());
		
		OntologyAccess ontologyAccess = getOntologyAccess(cv.getOntologyId());
		if(ontologyAccess != null) {
			term = ontologyAccess.getTermForAccession(cv.getAccession());
		} else {
			term = searchForTermByAccession(cv.getAccession());
		}
		
		return term;
	}

	
	Set<String> ontologyTermsToUrns(Collection<OntologyTermI> terms) {
		Set<String> urns = new HashSet<String>();
		
		for(OntologyTermI term : terms) {
			String ontologyID = term.getOntologyName();
			String accession = term.getTermAccession();
			String urn = miriam.getURI(ontologyID, accession);
			urns.add(urn);
		}
		
		return urns;
	}
	
	
/*
 * TODO Methods below would guarantee to work with BioPAX-recommended CVs...
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
