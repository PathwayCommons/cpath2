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

import java.io.IOException;

import cpath.service.CPathService;
import cpath.service.ErrorResponse;
import cpath.service.Status;
import cpath.service.jaxb.*;
import cpath.webservice.args.*;
import cpath.webservice.args.binding.*;

import org.biopax.paxtools.model.level3.Protein;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

/**
 * cPathSquared Search Web Service.
 * 
 * @author rodche
 */
@Controller
public class SearchController extends BasicController {
    private static final Logger log = LoggerFactory.getLogger(SearchController.class);    
	
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
    public @ResponseBody ServiceResponse search(@Valid Search search, 
    		BindingResult bindingResult, HttpServletRequest request, HttpServletResponse response) 
    			throws IOException
    {		
		if(!search.getQ().startsWith("*")) //a quick fix: do not log UI and not specific requests
			logHttpRequest(request);
    	
    	if(bindingResult.hasErrors()) {
			errorResponse(Status.BAD_REQUEST, 
					errorFromBindingResult(bindingResult), response);
			return null;
		} else {
			log.debug("/search called (for type: " 
				+ ((search.getType() == null)? "ALL" : search.getType()) 
				+ "), query:" + search.getQ() + ", page #" + search.getPage());

			// get results from the service
			ServiceResponse results = service.search(
					search.getQ(), search.getPage(), search.getType(),
					search.getDatasource(), search.getOrganism());

			if(results instanceof ErrorResponse) {
				errorResponse(Status.INTERNAL_ERROR, 
						((ErrorResponse) results).toString(), response);
			} else if(results.isEmpty()) {
				errorResponse(Status.NO_RESULTS_FOUND, 
						"no hits", response);
			} else {
				return results;
			}
			return null;
		}
	}
	
}