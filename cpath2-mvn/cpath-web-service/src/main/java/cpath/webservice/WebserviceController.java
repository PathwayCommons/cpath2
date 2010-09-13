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

import cpath.dao.PaxtoolsDAO;
import cpath.service.CPathService;
import cpath.service.CPathService.OutputFormat;
import cpath.service.CPathService.ResultMapKey;
import cpath.service.internal.CPathServiceImpl;
import cpath.service.internal.ProtocolStatusCode;
import cpath.warehouse.CvRepository;
import cpath.warehouse.internal.BioDataTypes;
import cpath.warehouse.internal.BioDataTypes.Type;
import cpath.webservice.args.*;
import cpath.webservice.args.binding.*;
import cpath.webservice.validation.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.level3.UtilityClass;
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
    // warehouse access objects:
    private PaxtoolsDAO proteinsDao;
    private PaxtoolsDAO moleculesDao;
    private CvRepository cvRepository;
    
	public WebserviceController(CPathServiceImpl service, PaxtoolsDAO proteinsDao,
			CvRepository cvRepository,  PaxtoolsDAO moleculesDao) {
		this.service = service;
		this.proteinsDao = proteinsDao;
		this.cvRepository = cvRepository;
		this.moleculesDao = moleculesDao;
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
        binder.registerCustomEditor(ProtocolVersion.class, new ProtocolVersionEditor());
    }

	
	/* ========================================================================
	 *    Most Important Web Service Methods
	 * ======================================================================*/
	
	// Get by ID
    @RequestMapping("/get")
    @ResponseBody
    public String elementById(
    		@RequestParam(value="format", required=false) OutputFormat format, 
    		@RequestParam("uri") String uri) 
    {
    	if (log.isDebugEnabled())
			log.debug("Query: /get; format:" + format + ", urn:" + uri);
    	
    	if(format==null) 
    		format = OutputFormat.BIOPAX;
    	
    	Map<ResultMapKey, Object> result = service.fetch(format, uri);
    	
    	String body = getBody(result, format, uri);
    	
		return body;
    }
	
	
    // Fulltext Search. TODO add organism and data sources filter args
    @RequestMapping(value="/search")
    @ResponseBody
    public String fulltextSearch(
    		@RequestParam(value="type", required=false) Class<? extends BioPAXElement> type, 
    		@RequestParam(value="q", required=true) String query)
    {		
    	if(type == null)
    		type = BioPAXElement.class;
    	
    	if(log.isDebugEnabled()) log.debug("Fulltext Search for type:" 
				+ type.getCanonicalName() + ", query:" + query);
    	Map<ResultMapKey,Object> results = service.find(query, type, false, null);
    	String body = getListDataBody(results, query + 
    			" (in " + type.getSimpleName() + ")");
		return body;
	}

    
	// Graph Queries
	@RequestMapping("/graph")
	@ResponseBody
    public String graphQuery(
    		@RequestParam(value="format", required=false) OutputFormat format,
    		@RequestParam(value="kind", required=true) GraphType kind,
    		@RequestParam(value="source", required=false) String sources,
    		@RequestParam(value="dest", required=false) String dests)
    {
		if(format==null) 
			format = OutputFormat.BIOPAX;
		
		StringBuffer toReturn = new StringBuffer(
				"(Graph Query Is Not Implemented Yet) " 
				+ "GraphQuery format:" + format + 
				", kind:" + kind + ", source:" 
				+ sources + ", dest:" + dests);
		
		if(log.isInfoEnabled()) 
			log.info(toReturn.toString());
		
		return toReturn.toString(); 
	}
	
	
	   
    /**
     * Controller for the legacy cPath web services
     * (backward compatibility).
     */
    /*
     * Currently, we do not use neither custom property editors nor framework's validator 
     * for the web method parameters. All the arguments are plain strings, 
     * and actual validation is performed after the binding, using the same approach 
     * as in old cPath (original cPath web 'protocol' was ported and re-factored)
     * 
     * TODO migrate to spring MVC and javax.validation framework (Validator, Errors and BindingResult, etc..)
     * 
     */
    @RequestMapping("/webservice.do")
    @ResponseBody
    public String doWebservice(
    		@RequestParam("cmd") String cmd, 
    		@RequestParam(value="version", required=false) String version,
    		@RequestParam("q") String q, // e.g. the list of identifiers or a search string
    		@RequestParam(value="output", required=false) String output,
    		@RequestParam(value="organism", required=false) String organism, // taxonomy id
    		@RequestParam(value="input_id_type", required=false) String inputIdType,
    		@RequestParam(value="data_source", required=false) String dataSources, //comma-separated names
    		@RequestParam(value="output_id_type", required=false) String outputIdType,
    		@RequestParam(value="binary_interaction_rule", required=false) String rules //comma-separated names
    	) 
    {
		String toReturn = "";
		final Map<String,String> argsMap = new HashMap<String, String>(); //map.toSingleValueMap();
		argsMap.put("cmd", cmd);
		argsMap.put("version", version);
		argsMap.put("q", q);
		argsMap.put("output", output);
		argsMap.put("organism", organism);
		argsMap.put("input_id_type", inputIdType);
		argsMap.put("data_source", dataSources);
		argsMap.put("output_id_type", outputIdType);
		argsMap.put("binary_interaction_rule", rules);
		ProtocolRequest protocol = null;
		
    	if(log.isDebugEnabled()) {
    		log.debug("After webservice.do request params binding - " + 
    				argsMap.toString());
    	}
		
    	// Validate the query parameters
		try {
			// build the ProtocolRequest from the Map
			protocol = new ProtocolRequest(argsMap);
			// validate with ProtocolValidator
			ProtocolValidator protocolValidator = new ProtocolValidator(protocol);
			protocolValidator.validate();
		} catch (ProtocolException e) {
			return ProtocolStatusCode.errorAsXml(e.getStatusCode(), 
						e.getMessage());
		}
		

		if(protocol.getCommand() == Cmd.SEARCH) {
			// return "forward:search.html"; // may try this later...
			// format is always 'xml' (the same as cpath webservice's)
			if(log.isDebugEnabled()) log.debug("Legacy (cpath) Fulltext Search:" 
					+ ", query:" + protocol.getQuery());
			// do cpath2 search query
	    	Map<ResultMapKey,Object> results = service
	    		.find(protocol.getQuery(), null, false, 
	    			new Integer[]{protocol.getOrganism()}, 
	    			protocol.getDataSources());
			if(results.containsKey(ResultMapKey.ERROR)) {
				return ProtocolStatusCode.errorAsXml(ProtocolStatusCode.INTERNAL_ERROR, 
						results.get(ResultMapKey.ERROR).toString());
			}
			
			// not found?
			if(results.isEmpty() || !results.containsKey(ResultMapKey.DATA)) {
				return ProtocolStatusCode.errorAsXml(ProtocolStatusCode.NO_RESULTS_FOUND,
					"No data returned for the search string '" 
						+ protocol.getQuery() + "'");
			}
			
			// convert the search result (id-list) to the legacy XML (SearchResponseType schema element)
			Collection<String> uris = (Collection<String>) results.get(ResultMapKey.DATA);
			// (reusing the same 'results' variable is intentional)
			results = service.fetchAsXmlSearchResponse(uris.toArray(new String[]{}));
			if(results.containsKey(ResultMapKey.ERROR)) {
				return ProtocolStatusCode.errorAsXml(ProtocolStatusCode.INTERNAL_ERROR, 
						results.get(ResultMapKey.ERROR).toString());
			}
			
			// Not found? (Converting the search result to the legacy web service XML format returned no data) 
			assert(results.containsKey(ResultMapKey.DATA)); // otherwise, it's a bug
			toReturn = (String) results.get(ResultMapKey.DATA);
			
		} else if(protocol.getCommand() == Cmd.GET_RECORD_BY_CPATH_ID) {
			//return "forward:get";
			return ProtocolStatusCode.errorAsXml(ProtocolStatusCode.INTERNAL_ERROR, 
					"Not Implemented Yet: legacy GET_RECORD_BY_CPATH_ID");
		} else if(protocol.getCommand() == Cmd.GET_BY_KEYWORD) {
			// probably, is the same as "search"
			return ProtocolStatusCode.errorAsXml(ProtocolStatusCode.INTERNAL_ERROR,
				"Not Implemented Yet: legacy GET_BY_KEYWORD");
		} else if(protocol.getCommand() == Cmd.GET_PATHWAYS) {
			return ProtocolStatusCode.errorAsXml(ProtocolStatusCode.INTERNAL_ERROR,
				"Not Implemented Yet: legacy GET_PATHWAYS");
		} else if(protocol.getCommand() == Cmd.GET_NEIGHBORS) {
			return ProtocolStatusCode.errorAsXml(ProtocolStatusCode.INTERNAL_ERROR,
				"Not Implemented Yet: legacy GET_NEIGHBORS");
		} else if(protocol.getCommand() == Cmd.GET_PARENTS) {
			//TODO implement "get_parents" or give up...
			// build a SummaryResponseType (from xsd), marshal
			return ProtocolStatusCode.errorAsXml(ProtocolStatusCode.INTERNAL_ERROR,
				"Not Implemented Yet: legacy GET_PARENTS");
		}
		
		return toReturn;
    }
	

	/* ========================================================================
	 *    The Rest of Web Methods 
	 * ======================================================================*/
	
	
	/**
	 * List of formats that web methods return
	 * 
	 * @return
	 */
    @RequestMapping("/help/formats")
    @ResponseBody
    public String getFormats() {
    	StringBuffer toReturn = new StringBuffer();
    	for(OutputFormat f : OutputFormat.values()) {
    		toReturn.append(f.toString().toUpperCase()).append(newline);
    	}
    	return toReturn.toString();
    }
    
    
    //=== Web methods that help understand BioPAX model (and rules) ===
    
	/**
	 * List of BioPAX L3 Classes
	 * 
	 * @return
	 */
    @RequestMapping("/help/types")
    @ResponseBody
    public String getBiopaxTypes() {
    	StringBuffer toReturn = new StringBuffer();
    	for(String type : BiopaxTypeEditor.getTypesByName().keySet()) {
    		toReturn.append(type).append(newline);
    	}
    	return toReturn.toString();
    }

      	
	/**
	 * List of bio-network data sources.
	 * 
	 * @return
	 */
    @RequestMapping("/help/datasources")
    @ResponseBody
    public String getDatasources() {
    	StringBuffer toReturn = new StringBuffer();
    	for(String ds : BioDataTypes.getDataSourceKeys(Type.PATHWAY_DATA)) {
    		toReturn.append(ds).append(newline);
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
						"No elements found for: " + details));
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
						"No elements found for: " + details);
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