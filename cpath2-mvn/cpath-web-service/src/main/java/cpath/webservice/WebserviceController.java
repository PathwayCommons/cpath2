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
import static cpath.service.CPathService.*;
import static cpath.service.CPathService.OutputFormat.*;
import cpath.service.CPathService.ResultMapKey;
import cpath.service.internal.CPathServiceImpl;
import cpath.service.internal.ProtocolStatusCode;
import cpath.webservice.args.*;
import cpath.webservice.args.binding.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.level3.Protein;
import org.biopax.paxtools.model.level3.UtilityClass;
import org.biopax.paxtools.query.algorithm.Direction;
import org.biopax.paxtools.query.algorithm.LimitType;
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
    		@RequestParam(value="type", required=false) Class<? extends BioPAXElement>[] types, 
    		@RequestParam(value="q", required=true) String query,
    		@RequestParam(value="organism", required=false) OrganismDataSource[] organisms,
    		@RequestParam(value="dataSource", required=false) PathwayDataSource[] dataSources)
    {		
    	String body = "";

		if (types == null || types.length == 0) {
			types = new Class[]{BioPAXElement.class};
			if (log.isInfoEnabled())
    			log.info("Type not specified/recognized;" +
    					" - using all (BioPAXElement).");
			//TODO distinguish between not specified vs. wrong type (better return error)
		}

		if (log.isDebugEnabled())
			log.debug("Fulltext Search (for " + Arrays.toString(types)
					+ "), query:" + query);

		
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
			service.find(query, types, taxons, dsources);
		
		body = getListDataBody(results, query + " (in " 
				+ Arrays.toString(types) + ")");

		return body;
	}

    
	//----- Graph Queries -------------------------------------------------------------------------|

	@RequestMapping("/graph")
	@ResponseBody
    public String graphQuery(
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