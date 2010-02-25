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

import java.util.Set;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.validator.impl.AbstractCvRule;
import org.biopax.validator.utils.CvTermsFetcher;
import org.springframework.beans.factory.annotation.Autowired;

import cpath.identity.BiopaxIdUtils;
import cpath.warehouse.beans.Cv;



/**
 * This is to access OBO Cvs:
 * TODO - uses PSIDEV's OntologyManager (via Validator's CvTermsFetcher)
 * TODO - re-uses BioPAX Validator's classes to extract only required by BioPAX CVs (though with synonyms and hierarchy)
 * 
 * @author rodch
 *
 */
public final class CvFetcher {
	private final static Log log = LogFactory.getLog(CvFetcher.class);
	
	private CvTermsFetcher cvTermsFetcher;
	private BiopaxIdUtils biopaxIdUtils;
	
	/*
	 * Injects Validator's CV rules that
	 * contain CV term restrictions for the 
	 * corresponding domain and property
	 * 
	 */
	@Autowired
	private Set<AbstractCvRule> cvRules;
	
	
	/**
	 * Constructor
	 * 
	 * @param cvTermsFetcher
	 * @param biopaxIdUtils
	 * @throws Exception
	 */
	public CvFetcher(CvTermsFetcher cvTermsFetcher, BiopaxIdUtils biopaxIdUtils) {
		this.biopaxIdUtils = biopaxIdUtils;
		this.cvTermsFetcher = cvTermsFetcher;
	}

	/**
	 * Gets all the existing CV rules
	 * 
	 * @return
	 */
	public Set<AbstractCvRule> getCvRules() {
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
	public Set<Cv> fetchBiopaxCVs(AbstractCvRule cvRule) {
		//TODO get the restrictions, then, restricted set of ontology terms
		//TODO convert each one ant its synonyms to the Cv bean; find and use URN as RDFId
		return null;
	}
		
}
