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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import cpath.dao.filters.SearchFilter;
import cpath.dao.internal.filters.EntityByOrganismRelationshipXrefsFilter;
import cpath.dao.internal.filters.EntityByProcessRelationshipXrefsFilter;
import cpath.dao.internal.filters.EntityDataSourceFilter;
import cpath.service.CPathService;
import static cpath.service.CPathService.*;
import static cpath.service.CPathService.OutputFormat.*;
import cpath.service.CPathService.ResultMapKey;
import cpath.service.internal.CPathServiceImpl;
import cpath.service.internal.ProtocolStatusCode;
import cpath.service.jaxb.ErrorType;
import cpath.service.jaxb.SearchHitType;
import cpath.service.jaxb.SearchResponseType;
import cpath.webservice.args.*;
import cpath.webservice.args.binding.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.level3.Entity;
import org.biopax.paxtools.model.level3.Protein;
import org.biopax.paxtools.query.algorithm.Direction;
import org.biopax.paxtools.query.algorithm.LimitType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import com.googlecode.ehcache.annotations.Cacheable;

import javax.validation.constraints.NotNull;

/**
 * cPathSquared Main Web Service.
 * 
 * @author rodche
 */
@Controller
public class WebserviceController {
    private static final Log log = LogFactory.getLog(WebserviceController.class);
    private static String newline = System.getProperty("line.separator");
    
    @NotNull
    private CPathService service; // main PC db access
    
    
    public WebserviceController() {
	}
    
    public WebserviceController(CPathServiceImpl service) {
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
        binder.registerCustomEditor(OutputFormat.class, new OutputFormatEditor());
        binder.registerCustomEditor(GraphType.class, new GraphTypeEditor());
        binder.registerCustomEditor(Direction.class, new GraphQueryDirectionEditor());
        binder.registerCustomEditor(LimitType.class, new GraphQueryLimitEditor());
        binder.registerCustomEditor(Class.class, new BiopaxTypeEditor());
        binder.registerCustomEditor(Cmd.class, new CmdEditor());
        binder.registerCustomEditor(CmdArgs.class, new CmdArgsEditor());
        binder.registerCustomEditor(OrganismDataSource.class, new OrganismDataSourceEditor());
        binder.registerCustomEditor(PathwayDataSource.class, new PathwayDataSourceEditor());
    }

	
	/* ========================================================================
	 *    Most Important Web Service Methods
	 * ======================================================================*/
	
	// Get by ID
    @RequestMapping("/get")
    public @ResponseBody String elementById(
    		@RequestParam(value="format", required=false) OutputFormat format, 
    		@RequestParam("uri") String[] uri) 
    {
    	if (log.isInfoEnabled())
			log.info("Query: /get; format:" + format + ", urn:" + uri);
    	
    	if(format==null) {
    		format = OutputFormat.BIOPAX;
    		if (log.isInfoEnabled())
    			log.info("Format not specified/recognized;" +
    					" - using BioPAX.");
    	}
    	
    	Map<ResultMapKey, Object> result = service.fetch(format, uri);
    	
    	String body = getBody(result, format, uri.toString());
    	
		return body;
    }
	
	
    // Fulltext Search
    @RequestMapping(value="/search")
    public @ResponseBody String fulltextSearch(
    		@RequestParam(value="type", required=false) Class<? extends BioPAXElement> type, 
    		@RequestParam(value="organism", required=false) OrganismDataSource[] organisms, //filter by
    		@RequestParam(value="datasource", required=false) PathwayDataSource[] dataSources, //filter by
    		@RequestParam(value="process", required=false) String[] pathwayURIs, // filter by
    		@RequestParam(value="q", required=true) String query
    		//,@RequestHeader("User-Agent") String userAgent //did not work as expected...
    		)
    {		
    	String body = "";

		if (log.isDebugEnabled())
			log.debug("/search called (for " + type	+ "), query:" + query);
		
		Set<SearchFilter> searchFilters = createFilters(organisms, dataSources, pathwayURIs);

		// get results from the service
		Map<ResultMapKey, Object> results = service.findElements(query, type, 
				searchFilters.toArray(new SearchFilter[]{}));
		
		String details = query + " (in " + type + ")";
		body = getListDataBody(results, details);
		
        /* hack to return "html" to browser so example on cpath-webdocs page
         * shows up without having to view page code - only required for safari
         */
		// did not work well...
        //return (userAgent.indexOf("Safari") != -1) ? getBodyAsHTML(body) : body;
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
				//organismURIs[i++] = o.asDataSource().getSystemCode(); // taxonomy id
				if(o == null)
					organismURIs[i++] = "UNKNOWN_THING"; // won't much anything!
				else
					organismURIs[i++] = o.getURI();
			}
			SearchFilter<Entity, String> byOrganismFilter = new EntityByOrganismRelationshipXrefsFilter();
			byOrganismFilter.setValues(organismURIs);
			searchFilters.add(byOrganismFilter);
		}
		
		if(dataSources != null) { // because of being optional arg.
			String[] dsourceURIs = new String[dataSources.length];
			int i = 0;
			for(PathwayDataSource d : dataSources) {
				//dsourceURIs[i++] = d.asDataSource().getSystemCode(); //just standard name
				//dsourceURIs[i++] = ((Provenance)d.asDataSource().getOrganism()).getRDFId(); // hack!
				if(d == null)
					dsourceURIs[i++] = "UNKNOWN_THING"; // won't much anything!
				else
					dsourceURIs[i++] = d.asDataSource().getSystemCode();
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


	/*
     * An alternative to /search command;
     * returns xml or json!
     * 
     */
    @Cacheable(cacheName = "findElementsCache")
    @RequestMapping(value="/xml/search")
    public @ResponseBody SearchResponseType find(
    		@RequestParam(value="type", required=false) Class<? extends BioPAXElement> type, 
    		@RequestParam(value="organism", required=false) OrganismDataSource[] organisms, //filter by
    		@RequestParam(value="datasource", required=false) PathwayDataSource[] dataSources, //filter by
    		@RequestParam(value="process", required=false) String[] pathwayURIs, // filter by
    		@RequestParam(value="q", required=true) String query
    )
    {		
		if (log.isDebugEnabled())
			log.debug("/find called (for " + type + "), query:" + query);
		
		SearchResponseType response = new SearchResponseType();
		
		Set<SearchFilter> searchFilters = createFilters(organisms, dataSources, pathwayURIs);

		// get results from the service
		Map<ResultMapKey, Object> results = service.findElements(query, type, 
				searchFilters.toArray(new SearchFilter[]{}));
		
		String details = query + " (in " + type + ")";

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
			ErrorType error = ProtocolStatusCode.INTERNAL_ERROR.createErrorType();
			error.setErrorDetails(results.get(ResultMapKey.ERROR).toString());
			response.setError(error);
		}
		
		return response;
	}

    
	/*
     * An alternative to /search command;
     * returns xml or json!
     * 
     */
    @RequestMapping(value="/xml/entity/search")
    public @ResponseBody SearchResponseType findEntities(
    		@RequestParam(value="type", required=false) Class<? extends BioPAXElement> type, 
    		@RequestParam(value="organism", required=false) OrganismDataSource[] organisms, //filter by
    		@RequestParam(value="datasource", required=false) PathwayDataSource[] dataSources, //filter by
    		@RequestParam(value="process", required=false) String[] pathwayURIs, // filter by
    		@RequestParam(value="q", required=true) String query
    )
    {		
		if (log.isDebugEnabled())
			log.debug("/find called (for " + type + "), query:" + query);
		
		SearchResponseType response = new SearchResponseType();
		
		Set<SearchFilter> searchFilters = createFilters(organisms, dataSources, pathwayURIs);

		// get results from the service
		Map<ResultMapKey, Object> results = service.findEntities(query, type, 
				searchFilters.toArray(new SearchFilter[]{}));
		
		String details = query + " (in " + type + ")";

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
			ErrorType error = ProtocolStatusCode.INTERNAL_ERROR.createErrorType();
			error.setErrorDetails(results.get(ResultMapKey.ERROR).toString());
			response.setError(error);
		}
		
		return response;
	}
    
	//----- Graph Queries -------------------------------------------------------------------------|

	@RequestMapping("/graph")
    public @ResponseBody String graphQuery(
		@RequestParam(value="format", required=false) OutputFormat format,
		@RequestParam(value="kind", required=true) GraphType kind, //required!
		@RequestParam(value="source", required=false) String[] source,
		@RequestParam(value="dest", required=false) String[] target,
		@RequestParam(value="limit", required=false, defaultValue = "1") Integer limit,
		@RequestParam(value="limit_type", required=false) LimitType limitType,
		@RequestParam(value="direction", required=false) Direction direction
		)
    {
		if(log.isInfoEnabled())
			log.info("GraphQuery format:" + format + ", kind:" + kind
				+ ((source == null) ? "no source nodes" : ", source:" + source.toString())
				+ ((target == null) ? "no target nodes" : ", target:" + target.toString())
				+ ", limit: " + limit
			);

		// set defaults
		if(format==null) { format = BIOPAX; }
		if(limit == null) { limit = 1; } 
		if(direction == null) { direction = Direction.DOWNSTREAM; }
		if(limitType == null) { limitType = LimitType.NORMAL; }
		
		String response = checkSourceAndLimit(source, limit);
		if (response != null) return response; // return error (xml)
		
		Map<ResultMapKey, Object> result;
		
		switch (kind) {
		case NEIGHBORHOOD:
			result = service.getNeighborhood(format, source, limit, direction);
			response = getBody(result, format, "nearest neighbors of " + source.toString());
			break;
		case PATHSBETWEEN:
			result = service.getPathsBetween(format, source, target, limit, limitType);
			response = getBody(result, format, "paths between " + source.toString()
				+ " and " + target.toString());
			break;
		case COMMONSTREAM:
			if (direction == Direction.BOTHSTREAM) {
				return ProtocolStatusCode.errorAsXml(ProtocolStatusCode.INVALID_ARGUMENT,
					"Direction parameter cannot be " + direction + " here");
			}
			result = service.getCommonStream(format, source, limit, direction);
			response = getBody(result, format, "common " + direction + "stream of " +
					source.toString());
			break;
		default:
			// impossible (should has failed earlier)
			break;
		}

		return response;
	}

	
	private String checkSourceAndLimit(String[] source, Integer limit)
	{
		String toReturn = null;

		if (source == null || source.length == 0)
		{
			toReturn = ProtocolStatusCode.errorAsXml(ProtocolStatusCode.MISSING_ARGUMENTS,
				"No source nodes specified for the neighborhood graph query.");
		}
		else if (limit < 0)
		{
			toReturn = ProtocolStatusCode.errorAsXml(ProtocolStatusCode.INVALID_ARGUMENT,
				"Search limit must be specified and must be non-negative");
		}

		return toReturn;
	}

	//---------------------------------------------------------------------------------------------|
	
    /**
     * makes a plain text string (response body) 
     * when the data (in the map) is the list of IDs,
     * one ID per line.
     * 
     * @deprecated
     */ 
	@Deprecated
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
			toReturn.append(ProtocolStatusCode
				.errorAsXml(ProtocolStatusCode.INTERNAL_ERROR, 
					result.get(ResultMapKey.ERROR).toString()));		
		}

        return toReturn.toString();
	}
  
    
    private String getBody(Map<ResultMapKey, Object> results, OutputFormat format, String details) {
    	String toReturn = null;
    	
		if (!results.containsKey(ResultMapKey.ERROR)) {
			toReturn = (String) results.get(ResultMapKey.DATA);
			if(toReturn == null) {
				toReturn = ProtocolStatusCode.errorAsXml(ProtocolStatusCode.NO_RESULTS_FOUND, 
						"Nothing found for: " + details);
			} 
		} else {
			toReturn = ProtocolStatusCode.errorAsXml(ProtocolStatusCode.INTERNAL_ERROR, 
					results.get(ResultMapKey.ERROR).toString());
		}
		
		return toReturn;
	}

    
    
    @Deprecated
    private String getBodyAsHTML(String body) {

        StringBuffer toReturn = new StringBuffer();

        toReturn.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">");
        toReturn.append("<body>");
        toReturn.append(body);
        toReturn.append("</body>");
        toReturn.append("</html>");

        // outta here
        return toReturn.toString();
    }

}