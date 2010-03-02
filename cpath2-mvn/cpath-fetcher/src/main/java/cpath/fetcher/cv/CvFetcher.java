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

package cpath.fetcher.cv;

import java.util.HashSet;
import java.util.Set;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.validator.impl.CvTermsRule;
import org.biopax.validator.utils.OntologyManagerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;

import psidev.psi.tools.ontology_manager.interfaces.OntologyTermI;

import cpath.identity.MiriamAdapter;
import cpath.warehouse.beans.Cv;



/**
 * This is to access OBO Cvs:
 * TODO - uses PSIDEV's OntologyManager (via Validator's OntologyManagerAdapter)
 * TODO - re-uses BioPAX Validator's classes to extract only required by BioPAX CVs (though with synonyms and hierarchy)
 * 
 * @author rodch
 *
 */
public final class CvFetcher extends OntologyManagerAdapter {
	private final static Log log = LogFactory.getLog(CvFetcher.class);
	
	private MiriamAdapter miriamAdapter;
	
	/*
	 * Injects Validator's CV rules that
	 * contain CV term restrictions for the 
	 * corresponding domain and property
	 * 
	 */
	@Autowired
	private Set<CvTermsRule> cvRules;
	
	
	/**
	 * Constructor
	 * 
	 * @param ontologies ontology config XML resource (for OntologyManager)
	 * @param miriam
	 * @throws Exception
	 */
	public CvFetcher(Resource ontologies, MiriamAdapter miriamAdapter) {
		super(ontologies);
		this.miriamAdapter = miriamAdapter;
	}

	/**
	 * Gets all the existing CV rules
	 * 
	 * @return
	 */
	public Set<CvTermsRule> getCvRules() {
		return cvRules;
	}
	
	
	/**
	 * Fetches CPathWarehouse's controlled vocabulary beans 
	 * using constraints defined by the specific validation rule.
	 * 
	 * @param domain
	 * @param property
	 * @return
	 */
	public Set<Cv> fetchBiopaxCVs(CvTermsRule cvRule) {
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
		String urn = miriamAdapter.getURI(ontology, accession);
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
			
}
