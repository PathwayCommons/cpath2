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
import org.biopax.paxtools.model.level3.UtilityClass;
import org.bridgedb.bio.Organism;
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
    		@RequestParam(value="organism", required=false) Organism[] organisms,
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

		Integer[] taxons = new Integer[organisms.length];
		int i = 0;
		for(Organism o : organisms) {
			taxons[i++] = Integer.valueOf(o.code());
		}
		
		Map<ResultMapKey, Object> results = null; //TODO
			//service.find(query, type, false, taxons, dataSources);
		
		body = getListDataBody(results, query + " (in " + type.getSimpleName()
				+ ")");

		return body;
	}

    
	// Graph Queries
	@RequestMapping("/graph")
	@ResponseBody
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
    			log.info("Format not specified/recognized;" +
    					" - using BioPAX.");
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