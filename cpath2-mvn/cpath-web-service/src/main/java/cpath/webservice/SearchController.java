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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import cpath.dao.filters.SearchFilter;
import cpath.dao.internal.filters.EntityByOrganismRelationshipXrefsFilter;
import cpath.dao.internal.filters.EntityByProcessRelationshipXrefsFilter;
import cpath.dao.internal.filters.EntityDataSourceFilter;
import static cpath.service.CPathService.*;
import cpath.service.CPathService;
import cpath.service.CPathService.ResultMapKey;
import cpath.service.internal.ProtocolStatusCode;
import cpath.service.jaxb.ErrorType;
import cpath.service.jaxb.SearchHitType;
import cpath.service.jaxb.SearchResponseType;
import cpath.webservice.args.*;
import cpath.webservice.args.binding.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.model.level3.Entity;
import org.biopax.paxtools.model.level3.Protein;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * cPathSquared Main Web Service.
 * 
 * @author rodche
 */
@Controller
public class SearchController extends BasicController {
    private static final Log log = LogFactory.getLog(SearchController.class);    
	
	
    public SearchController(CPathService service) {
		this.service = service;
	}
    
    /**
	 * This configures the web request parameters binding, i.e., 
	 * conversion to the corresponding java types; for example,
	 * "neighborhood" is recognized as {@link GraphType#NEIGHBORHOOD}, 
	 * "search" will become {@link Cmd#SEARCH}, 
	 *  "protein" - {@link Protein} , etc.
	 *  Depending on the editor, illegal query parameters may result 
	 *  in an error or just NULL value (see e.g., {@link CmdArgsEditor})
	 * 
	 * @param binder
	 */
	@InitBinder
    public void initBinder(WebDataBinder binder) {
		super.initBinder(binder);
		//TODO set SearchValidator
    }

	
	/* ========================================================================
	 *    Most Important Web Service Methods
	 * ======================================================================*/
	
	
	// Get by ID (URI) command
    @RequestMapping("/get")
    public @ResponseBody String elementById(@Valid Get get, BindingResult binding) 
    {
    	if(binding.hasErrors()) {
    		return ProtocolStatusCode.errorAsXml(ProtocolStatusCode.BAD_REQUEST,
					"Missing parameter 'uri': at least one URI must be specified!");
    	}

    	
    	OutputFormat format = get.getFormat();
    	String[] uri = get.getUri();
    	
    	if (log.isInfoEnabled())
			log.info("Query: /get; format:" + format + ", urn:" + uri);

    	
    	if(format==null) {
    		format = OutputFormat.BIOPAX;
    		log.info("Using default output format (BioPAX), " +
    			"because nothing was specified/recognized.");
    	}
    	
    	Map<ResultMapKey, Object> result = service.fetch(format, uri);
    	
    	String body = getBody(result, format, Arrays.toString(uri));
    	
		return body;
    }
	
	
   
    // Fulltext Search - plain text response...
    @Deprecated
    @RequestMapping(value="/search")
    public @ResponseBody String fulltextSearch(@Valid Search search, BindingResult bindingResult)
    {		
		if(bindingResult.hasErrors()) {
			ErrorType error = errorfromBindingResult(bindingResult);
			return ProtocolStatusCode.marshal(error);
		}
		
		String body = "";
    	
    	if (log.isDebugEnabled())
			log.debug("/search called (for " + search.getType()	+ "), query:" + search.getQ());
		
		Set<SearchFilter> searchFilters = createFilters(
			search.getOrganisms(), search.getDataSources(), search.getPathwayURIs());

		// get results from the service
		Map<ResultMapKey, Object> results = service.findElements(
				search.getQ(), search.getType(), 
				searchFilters.toArray(new SearchFilter[]{}));
		
		String details = search.getQ() + " (in " + search.getType() + ")";
		body = getListDataBody(results, details);
		
		return body;
	}   
 
    
    private Set<SearchFilter> createFilters(OrganismDataSource[] organisms,
			PathwayDataSource[] dataSources, String[] pathwayURIs) 
	{
		Set<SearchFilter> searchFilters = new HashSet<SearchFilter>();
		if(organisms != null) { // it's optional parameter (can be null)
			String[] organismURIs = new String[organisms.length];
			int i = 0;
			for(OrganismDataSource o : organisms) {
				organismURIs[i++] = (o == null) ? "UNKNOWN" : o.getURI();
			}
			SearchFilter<Entity, String> byOrganismFilter = new EntityByOrganismRelationshipXrefsFilter();
			byOrganismFilter.setValues(organismURIs);
			searchFilters.add(byOrganismFilter);
		}
		
		if(dataSources != null) { // because of being optional arg.
			String[] dsourceURIs = new String[dataSources.length];
			int i = 0;
			for(PathwayDataSource d : dataSources) {
				dsourceURIs[i++] = (d == null) ? "UNKNOWN" : d.asDataSource().getSystemCode();
			}
			SearchFilter<Entity, String> byDatasourceFilter = new EntityDataSourceFilter();
			byDatasourceFilter.setValues(dsourceURIs);
			searchFilters.add(byDatasourceFilter);
		}
		
		if(pathwayURIs != null) {
			SearchFilter<Entity, String> byProcessFilter = new EntityByProcessRelationshipXrefsFilter();
			byProcessFilter.setValues(pathwayURIs);
			searchFilters.add(byProcessFilter);
		}
		
		return searchFilters;
	}


    @RequestMapping(value="/find")
    public @ResponseBody SearchResponseType find(@Valid Search search, BindingResult bindingResult)
    {		
    	SearchResponseType response = new SearchResponseType();
    	
		if(bindingResult.hasErrors()) {
			response.setError(errorfromBindingResult(bindingResult));
			return response; // return ERROR
		}
		
		if (log.isDebugEnabled())
			log.debug("/find called (for " + search.getType() + "), query:" + search.getQ());
		
		Set<SearchFilter> searchFilters = createFilters(
				search.getOrganisms(), search.getDataSources(), search.getPathwayURIs());

		// get results from the service
		Map<ResultMapKey, Object> results = service.findElements(
				search.getQ(), search.getType(), 
				searchFilters.toArray(new SearchFilter[]{}));
		
		String details = search.getQ() + " (in " + search.getType() + ")";

		if (!results.containsKey(ResultMapKey.ERROR)) {
			List<SearchHitType> dataSet = (List<SearchHitType>) results.get(ResultMapKey.DATA);
			if(dataSet.isEmpty()) {
				ErrorType error = ProtocolStatusCode.NO_RESULTS_FOUND.createErrorType();
				error.setErrorDetails("Nothing found for: " + details);
				response.setError(error);
			} else {
				response.setTotalNumHits((long) dataSet.size());
				response.getSearchHit().addAll(dataSet);
			}
		} else {
			response.setError(
					errorFromResults(results.get(ResultMapKey.ERROR), 
					ProtocolStatusCode.INTERNAL_ERROR)
			);
		}
		
		return response;
	}

    
	@RequestMapping(value="/entity/find")
    public @ResponseBody SearchResponseType findEntities(@Valid Search search, BindingResult bindingResult)
    {		
    	SearchResponseType response = new SearchResponseType();
    	
		if(bindingResult.hasErrors()) {
			response.setError(errorfromBindingResult(bindingResult));
			return response; // return ERROR
		}
		
		if (log.isDebugEnabled())
			log.debug("/entity/find called (for " + search.getType() + "), query:" + search.getQ());
		
		Set<SearchFilter> searchFilters = createFilters(
				search.getOrganisms(), search.getDataSources(), search.getPathwayURIs());

		// get results from the service
		Map<ResultMapKey, Object> results = service.findEntities(
				search.getQ(), search.getType(), 
				searchFilters.toArray(new SearchFilter[]{}));
		
		String details = search.getQ() + " (in " + search.getType() + ")";

		if (!results.containsKey(ResultMapKey.ERROR)) {
			List<SearchHitType> dataSet = (List<SearchHitType>) results.get(ResultMapKey.DATA);
			if(dataSet.isEmpty()) {
				ErrorType error = ProtocolStatusCode.NO_RESULTS_FOUND.createErrorType();
				error.setErrorDetails("Nothing found for: " + details);
				response.setError(error);
			} else {
				response.setTotalNumHits((long) dataSet.size());
				response.getSearchHit().addAll(dataSet);
			}
		} else {
			response.setError(
					errorFromResults(results.get(ResultMapKey.ERROR), 
					ProtocolStatusCode.INTERNAL_ERROR)
			);
		}
		
		return response;
	}
    
    	
    /**
     * makes a plain text string (response body) 
     * when the data (in the map) is the list of IDs,
     * one ID per line.
     * 
     */ 
    private String getListDataBody(Map<ResultMapKey, Object> result, String details) {
    	StringBuffer toReturn = new StringBuffer();
    	
		if (!result.containsKey(ResultMapKey.ERROR)) {
			List<SearchHitType> dataSet = (List<SearchHitType>) result.get(ResultMapKey.DATA);
			if(dataSet.isEmpty()) {
				toReturn.append(ProtocolStatusCode
					.errorAsXml(ProtocolStatusCode.NO_RESULTS_FOUND, 
						"Nothing found for: " + details));
			} else {
				for (SearchHitType s : dataSet) {
					toReturn.append(s.getUri()).append(newline);
				}
			}
		} else {
			toReturn.append(
				ProtocolStatusCode.marshal(
					errorFromResults(result.get(ResultMapKey.ERROR), ProtocolStatusCode.INTERNAL_ERROR)
				)
			);		
		}

        return toReturn.toString();
	}

}