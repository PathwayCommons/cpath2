/**
 ** Copyright (c) 2010 Memorial Sloan-Kettering Cancer Center (MSKCC)
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

package cpath.webservice;

import cpath.service.CPathService;
import cpath.service.jaxb.*;
import cpath.webservice.args.*;
import cpath.webservice.args.binding.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.model.level3.Protein;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.Valid;

/**
 * cPathSquared Search Web Service.
 * 
 * @author rodche
 */
@Controller
public class SearchController extends BasicController {
    private static final Log log = LogFactory.getLog(SearchController.class);    
	
    private CPathService service; // main PC db access
	
    public SearchController(CPathService service) {
		this.service = service;
	}
    
    /**
	 * This configures the web request parameters binding, i.e., 
	 * conversion to the corresponding java types; for example,
	 * "protein" is recognized as {@link Protein}, etc.
	 *  Illegal query parameters result in binding errors.
	 * 
	 * @param binder
	 */
	@InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Class.class, new BiopaxTypeEditor());
    }


    @RequestMapping(value="/search")
    public @ResponseBody ServiceResponse search(@Valid Search search, BindingResult bindingResult)
    {		
		if(bindingResult.hasErrors()) {
			return errorfromBindingResult(bindingResult);
		} else {
			if (log.isDebugEnabled())
				log.debug("/search called (for type: " 
					+ ((search.getType() == null)? "ALL" : search.getType()) 
					+ "), query:" + search.getQ() + ", page #" + search.getPage());

			// get results from the service
			ServiceResponse results = service.search(
					search.getQ(), search.getPage(), search.getType(),
					search.getDatasource(), search.getOrganism());

			return results;
		}
	}
	
}