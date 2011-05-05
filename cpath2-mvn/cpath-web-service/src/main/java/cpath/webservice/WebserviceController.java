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
import cpath.service.CPathService.OutputFormat;
import cpath.service.CPathService.ResultMapKey;
import cpath.service.internal.CPathServiceImpl;
import cpath.service.internal.ProtocolStatusCode;
import cpath.webservice.args.*;
import cpath.webservice.args.binding.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.level3.BioSource;
import org.biopax.paxtools.model.level3.Provenance;
import org.biopax.paxtools.model.level3.UtilityClass;
import org.biopax.paxtools.query.algorithm.CommonStreamQuery;
import org.biopax.paxtools.query.algorithm.PoIQuery;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

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
    
    
	public WebserviceController(CPathServiceImpl service) {
		this.service = service;
	}

	
	/**
	 * Customizes request parameters conversion to proper internal types,
	 * e.g., "network of interest" is recognized as GraphType.NETWORK_OF_INTEREST, etc.
	 * 
	 * @param binder
	 */
	@InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(OutputFormat.class, new OutputFormatEditor());
        binder.registerCustomEditor(GraphType.class, new GraphTypeEditor());
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
    @ResponseBody
    public String elementById(
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
    @ResponseBody
    public String fulltextSearch(
    		@RequestParam(value="type", required=false) Class<? extends BioPAXElement> type, 
    		@RequestParam(value="q", required=true) String query,
    		@RequestParam(value="organism", required=false) OrganismDataSource[] organisms,
    		@RequestParam(value="dataSource", required=false) PathwayDataSource[] dataSources)
    {		
    	String body = "";

		if (type == null) {
			type = BioPAXElement.class;
			if (log.isInfoEnabled())
    			log.info("Type not specified/recognized;" +
    					" - using all (BioPAXElement).");
			//TODO distinguish between not specified vs. wrong type (better return error)
		}

		if (log.isDebugEnabled())
			log.debug("Fulltext Search for type:" + type.getCanonicalName()
					+ ", query:" + query);

		
		String[] taxons = null; 
		if(organisms != null) { // it's optional parameter (can be null)
			taxons = new String[organisms.length];
			int i = 0;
			for(OrganismDataSource o : organisms) {
				taxons[i++] = o.asDataSource().getSystemCode(); // taxonomy id
				//taxons[i++] = o.asDataSource().getURN(o.asDataSource().getSystemCode()); //Miriam URN
				//taxons[i++] = ((BioSource)o.asDataSource().getOrganism()).getRDFId(); //Miriam URN
			}
		}
		
		String[] dsources = null; 
		if(dataSources != null) { // because of being optional arg.
			dsources = new String[dataSources.length];
			int i = 0;
			for(PathwayDataSource o : dataSources) {
				dsources[i++] = o.asDataSource().getSystemCode(); //standard name
				//dsources[i++] = o.asDataSource().getURN(""); //Miriam URN
				//dsources[i++] = ((Provenance)o.asDataSource().getOrganism()).getRDFId(); // hack!
			}
		}
		
		Map<ResultMapKey, Object> results = 
			service.find(query, type, false, taxons, dsources);
		
		body = getListDataBody(results, query + " (in " + type.getSimpleName()
				+ ")");

		return body;
	}

    
	//----- Graph Queries -------------------------------------------------------------------------|

	// Old nearest neighbor query
	@RequestMapping("/graph")
	@ResponseBody
	/**
	 * @deprecated use the method below this one
	 */
    public String graphQuery(
		@RequestParam(value="format", required=false) OutputFormat format,
		@RequestParam(value="kind", required=true) GraphType kind,
		@RequestParam(value="source", required=false) String[] sources,
		@RequestParam(value="dest", required=false) String[] dests)
    {
		String toReturn = "";

		if(log.isInfoEnabled())
			log.info("GraphQuery format:" + format + ", kind:" + kind
				+ ( (sources == null) ? "no source nodes" : ", source:" + sources.toString() )
				+ ( (dests == null) ? "no dest. nodes" : ", dest:" + dests.toString()) );

		if(format==null)  {
				format = OutputFormat.BIOPAX;
				if (log.isInfoEnabled())
				log.info("Format not specified/recognized; - using BioPAX.");
		}

		if(kind == GraphType.NEIGHBORHOOD) {
			if (sources != null && sources.length > 0) {
				Map<ResultMapKey, Object> result = service.getNeighborhood(
					format, sources);
				toReturn = getBody(result, format, "nearest neighbors of "
					+ sources.toString());
			} else {
				toReturn = ProtocolStatusCode.errorAsXml(ProtocolStatusCode.MISSING_ARGUMENTS,
					"No source nodes specified for the neighborhood graph query.");
			}
		}

		return toReturn.toString();
	}

	// I tried to map the below "/neighborhood" string to the GraphQueryType.NEIGHBORHOOD enum, but
	// it didn't work. @RequestMapping requests a *constant* string and it seems that using enum in
	// any way violates this.

	// Neighborhood query
	@RequestMapping("/" + GraphType.NEIGHBORHOOD_STR)
	@ResponseBody
    public String neighborhoodQuery(
		@RequestParam(value="format", defaultValue = OutputFormat.BIOPAX_STR) OutputFormat format,
		@RequestParam(value="source", required=true) String[] source,
		@RequestParam(value="limit", defaultValue = "1") Integer limit,
		@RequestParam(value="direction") GraphQueryParameter direction)
    {
		String response = checkFormatSourceLimit(format, source, limit);
		if (response != null) return response;

		if (direction != GraphQueryParameter.UPSTREAM &&
			direction != GraphQueryParameter.DOWNSTREAM &&
			direction != GraphQueryParameter.BOTHSTREAM)
		{
			return ProtocolStatusCode.errorAsXml(ProtocolStatusCode.INVALID_ARGUMENT,
				"direction parameter should be " + GraphQueryParameter.UPSTREAM + " or " +
					GraphQueryParameter.DOWNSTREAM + " or " + GraphQueryParameter.BOTHSTREAM);
		}

		boolean upstream = direction == GraphQueryParameter.UPSTREAM ||
			direction == GraphQueryParameter.BOTHSTREAM;

		boolean downstream = direction == GraphQueryParameter.DOWNSTREAM ||
			direction == GraphQueryParameter.BOTHSTREAM;

		Map<ResultMapKey, Object> result =
			service.getNeighborhood(format, source, limit, upstream, downstream);

		response = getBody(result, format, "nearest neighbors of " + source.toString());

		return response;
	}

	// Paths-between query
	@RequestMapping("/" + GraphType.PATHSBETWEEN_STR)
	@ResponseBody
    public String pathsBetweenQuery(
		@RequestParam(value="format", defaultValue = OutputFormat.BIOPAX_STR) OutputFormat format,
		@RequestParam(value="source", required=true) String[] source,
		@RequestParam(value="target", required=false) String[] target,
		@RequestParam(value="limit", defaultValue = "1") Integer limit,
		@RequestParam(value="limit_type", defaultValue = "normal")
			GraphQueryParameter limitType)
    {
		String response = checkFormatSourceLimit(format, source, limit);
		if (response != null) return response;

		if (limitType != GraphQueryParameter.NORMAL &&
			limitType != GraphQueryParameter.SHORTEST_PLUS_K)
		{
			return ProtocolStatusCode.errorAsXml(ProtocolStatusCode.INVALID_ARGUMENT,
				"limit_type must be either " + GraphQueryParameter.NORMAL + " or " +
					GraphQueryParameter.SHORTEST_PLUS_K);
		}

		boolean limitT = limitType == GraphQueryParameter.NORMAL ?
			PoIQuery.NORMAL_LIMIT : PoIQuery.SHORTEST_PLUS_K;

		Map<ResultMapKey, Object> result =
			service.getPathsBetween(format, source, target, limit, limitT);

		response = getBody(result, format, "paths between " + source.toString() + " and " +
			target.toString());

		return response;
	}

	// Common stream query
	@RequestMapping("/" + GraphType.COMMONSTREAM_STR)
	@ResponseBody
    public String commonStreamQuery(
		@RequestParam(value="format", defaultValue = OutputFormat.BIOPAX_STR) OutputFormat format,
		@RequestParam(value="source", required=true) String[] source,
		@RequestParam(value="limit", defaultValue = "1") Integer limit,
		@RequestParam(value="direction", defaultValue = "downstream")
			GraphQueryParameter direction)
    {
		String response = checkFormatSourceLimit(format, source, limit);
		if (response != null) return response;

		if (direction != GraphQueryParameter.UPSTREAM &&
			direction != GraphQueryParameter.DOWNSTREAM)
		{
			return ProtocolStatusCode.errorAsXml(ProtocolStatusCode.INVALID_ARGUMENT,
				"direction parameter should be either " + GraphQueryParameter.UPSTREAM + " or " +
					GraphQueryParameter.DOWNSTREAM);
		}

		boolean dir = direction == GraphQueryParameter.UPSTREAM ?
			CommonStreamQuery.UPSTREAM : CommonStreamQuery.DOWNSTREAM;

		Map<ResultMapKey, Object> result =
			service.getCommonStream(format, source, limit, dir);

		response = getBody(result, format, "common " + (dir ? "down" : "up") + "stream of " +
			source.toString());

		return response;
	}

	private String checkFormatSourceLimit(OutputFormat format, String[] source, Integer limit)
	{
		String toReturn = null;

		if(log.isInfoEnabled())
			log.info("GraphQuery format:" + format
				+ ((source == null) ? "no source nodes" : ", source:" + source.toString()
				+ ", limit: " + limit));

		if(format == null)
		{
			if (log.isInfoEnabled()) log.info("Format not specified/recognized -- using BioPAX.");
		}

		if (source == null || source.length == 0)
		{
			toReturn = ProtocolStatusCode.errorAsXml(ProtocolStatusCode.MISSING_ARGUMENTS,
				"No source nodes specified for the neighborhood graph query.");
		}
		else if (limit == null || limit < 0)
		{
			toReturn = ProtocolStatusCode.errorAsXml(ProtocolStatusCode.INVALID_ARGUMENT,
				"Search limit must be specified and must be non-negative");
		}

		return toReturn;
	}

	//---------------------------------------------------------------------------------------------|
	
    /*
     * makes a plain text string (response body) 
     * when the data (in the map) is the list of IDs,
     * one ID per line.
     */
    private String getListDataBody(Map<ResultMapKey, Object> result, String details) {
    	StringBuffer toReturn = new StringBuffer();
    	
		if (!result.containsKey(ResultMapKey.ERROR)) {
			Collection<String> dataSet = (Collection<String>) result.get(ResultMapKey.DATA);
			if(dataSet.isEmpty()) {
				toReturn.append(ProtocolStatusCode
					.errorAsXml(ProtocolStatusCode.NO_RESULTS_FOUND, 
						"Nothing found for: " + details));
			} else {
				for (String s : dataSet) {
					toReturn.append(s).append(newline);
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

    
    //TODO the following two methods were copied from the (removed) webservice2Controller and require some coding...
	/*
	 * Gets the utility element (from warehouse) by ID.
	 * 
	 * TODO implement...
	 */
    String getElementsOfType(Class<? extends BioPAXElement> type, String urn) 
    {
    	if(type == null) {
    		type = UtilityClass.class;
    	} else if(!UtilityClass.class.isAssignableFrom(type)) {
    		log.warn("Parameter 'type' value, " + 
    				type.getSimpleName() + ", is not a sub class of UtilityClass " +
    				"(UtilityClass will be used for the search instead)!");
    		type = UtilityClass.class;
    	}
    	
    	if(log.isInfoEnabled()) 
    		log.info("Warehouse query for type:" + type.getSimpleName() 
    			+ ", urn:" + urn);
    	
    	
    	StringBuffer toReturn = new StringBuffer();
		if (UtilityClass.class.isAssignableFrom(type)) {
			//TODO get from warehouse
			UtilityClass el = null;
			
			if(el != null) {
				toReturn.append(el.getRDFId()).append(newline);
			}
		}
		
    	return toReturn.toString();
    }
    
    
    /*
     * Search Warehouse for utility class elements 
     * (for those not found in the main PC model storage)
     * 
     * TODO implement....
     */
    String fulltextSearchForType(Class<? extends BioPAXElement> type, String query) 
    {	
    	if(type == null) {
    		type = UtilityClass.class;
    	} else if(!UtilityClass.class.isAssignableFrom(type)) {
    		log.warn("Parameter 'type' value, " + 
    				type.getSimpleName() + ", is not a sub class of UtilityClass " +
    				"(UtilityClass will be used for the search instead)!");
    		type = UtilityClass.class;
    	}
    	
    	if(log.isInfoEnabled()) log.info("Warehouse fulltext Search for type:" 
				+ type.getCanonicalName() + ", query:" + query);
    	
    	StringBuffer toReturn = new StringBuffer();
    	
    	/*
    	 * TODO search Warehouse
		*/
		
		return toReturn.toString(); 
	}

}