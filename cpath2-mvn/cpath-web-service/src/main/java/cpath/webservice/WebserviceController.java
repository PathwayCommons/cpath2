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

import cpath.dao.CPathService.OutputFormat;
import cpath.dao.CPathService.ResultMapKey;
import cpath.dao.internal.CPathServiceImpl;
import cpath.warehouse.internal.BioDataTypes;
import cpath.warehouse.internal.BioDataTypes.Type;
import cpath.webservice.args.*;
import cpath.webservice.args.binding.*;
import cpath.webservice.jaxb.ErrorType;
import cpath.webservice.validation.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.model.BioPAXElement;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.io.*;
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
    private CPathServiceImpl service;
    
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
        binder.registerCustomEditor(BioPAXElement.class, new BiopaxTypeEditor());
        binder.registerCustomEditor(Cmd.class, new CmdEditor());
        binder.registerCustomEditor(ProtocolVersion.class, new ProtocolVersionEditor());
    }
	
	
	/**
	 * List of formats that web methods return
	 * 
	 * @return
	 */
    @RequestMapping("/formats")
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
    @RequestMapping("/types")
    @ResponseBody
    public String getBiopaxTypes() {
    	StringBuffer toReturn = new StringBuffer();
    	for(String type : BiopaxTypeEditor.getTypesByName().keySet()) {
    		toReturn.append(type).append(newline);
    	}
    	return toReturn.toString();
    }

    
    //=== Web methods that list all BioPAX element or - by type ===
    
    /*
     * TODO all objects?.. This might be too much to ask :)
     */
    @RequestMapping(value="/all/elements")
    @ResponseBody
    public String getElements() throws IOException {
    	return getElementsOfType(BioPAXElement.class);
    }

    
    @RequestMapping(value="/types/{type}/elements")
    @ResponseBody
    public String getElementsOfType(@PathVariable("type") Class<? extends BioPAXElement> type) {
    	Map<ResultMapKey, Object> results = service.list(type, false);
    	String body = getListDataBody(results, type.getSimpleName());
		return body;
    }
    
     
    /*=== Most critical web method that get ONE element by ID (URI) ===*/
	@RequestMapping(value="/elements")
    @ResponseBody
    public String elementById(@RequestParam("uri") String uri) {
    	if(log.isInfoEnabled()) log.info("Query /elements");
    	return elementById(OutputFormat.BIOPAX, uri);
    }

    
    @RequestMapping(value="/format/{format}/elements")
    @ResponseBody
    public String elementById(@PathVariable("format") OutputFormat format, 
    		@RequestParam("uri") String uri) 
    {
    	if (log.isDebugEnabled())
			log.debug("Query - format:" + format + ", urn:" + uri);
    	Map<ResultMapKey, Object> result = service.element(uri, format);
    	String body = getBody(result, format, uri);
		return body;
    }
    
    
    //=== Fulltext search web methods ===//
    
	@RequestMapping(value="/find/{query}")
	@ResponseBody
    public String fulltextSearch(@PathVariable("query") String query) {
		return fulltextSearchForType(BioPAXElement.class, query);
	}
        

    @RequestMapping(value="/types/{type}/find/{query}")
    @ResponseBody
    public String fulltextSearchForType(
    		@PathVariable("type") Class<? extends BioPAXElement> type, 
    		@PathVariable("query") String query) 
    {		
    	if(log.isInfoEnabled()) log.info("Fulltext Search for type:" 
				+ type.getCanonicalName() + ", query:" + query);
    	Map<ResultMapKey,Object> results = service.list(query, type);
    	String body = getListDataBody(results, query + 
    			" (in " + type.getSimpleName() + ")");
		return body;
	}
    
	
    // TODO later, remove "method = RequestMethod.POST" and the test form method below
	@RequestMapping(value="/graph", method = RequestMethod.POST)
	@ResponseBody
    public String graphQuery(@RequestBody MultiValueMap<String, String> formData)
    {
		if(log.isInfoEnabled()) log.info("GraphQuery format:" 
				+ formData.get("format") + ", kind:" + formData.get("kind") 
				+ ", source:" + formData.get("source") 
				+ ", dest:" + formData.get("dest"));
		
		StringBuffer toReturn = new StringBuffer("Not Implemented Yet :)" 
				+ "GraphQuery format:" + formData.get("format") + 
				", kind:" + formData.get("kind") + ", source:" 
				+ formData.get("source") + ", dest:" 
				+ formData.get("dest"));
		
		return toReturn.toString(); 
	}
	
	
	// temporary - for testing
	@RequestMapping(value="/graph", method = RequestMethod.GET)
    public void testForm() {}
	
	
	/**
	 * List of bio-network data sources.
	 * 
	 * @return
	 */
    @RequestMapping("/datasources")
    @ResponseBody
    public String getDatasources() {
    	StringBuffer toReturn = new StringBuffer();
    	for(String ds : BioDataTypes.getDataSourceKeys(Type.PATHWAY_DATA)) {
    		toReturn.append(ds).append(newline);
    	}
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
    		/*
    		@RequestParam("cmd") String cmd, 
    		@RequestParam(value="version", required=false) String version,
    		@RequestParam("q") String q, // e.g. the list of identifiers or a search string
    		@RequestParam(value="output", required=false) String output,
    		@RequestParam(value="organism", required=false) String organism, // taxonomy id
    		@RequestParam(value="input_id_type", required=false) String inputIdType,
    		@RequestParam(value="data_source", required=false) String dataSources, //comma-separated names
    		@RequestParam(value="output_id_type", required=false) String outputIdType,
    		@RequestParam(value="binary_interaction_rule", required=false) String rules //comma-separated names
    		*/
    		@RequestBody MultiValueMap<String,String> map
    	) 
    {
    	if(log.isDebugEnabled()) {
    		log.debug("After webservice.do request params binding - " + 
    				map.toString());
    	}

		String toReturn = "";
		try {
			// build the ProtocolRequest from the Map
			ProtocolRequest request = new ProtocolRequest(map.toSingleValueMap());
			// validate by ProtocolValidator
			ProtocolValidator protocolValidator = new ProtocolValidator(request);
			protocolValidator.validate();
		} catch (ProtocolException e) {
			ErrorType errorType = e.getStatusCode().createErrorType();
			// set the error details
			errorType.setErrorDetails(e.getMessage());
			// build the xml string
			return ProtocolStatusCode.marshal(errorType);
		}

		// TODO execute query and get results here

		return toReturn;
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
				ErrorType errorType = ProtocolStatusCode.NO_RESULTS_FOUND.createErrorType();
				errorType.setErrorDetails("No elements found for: " + details);
				toReturn.append(ProtocolStatusCode.marshal(errorType));
			} else {
				for (String s : dataSet) {
					toReturn.append(s).append(newline);
				}
			}
		} else {
			ErrorType errorType = ProtocolStatusCode.INTERNAL_ERROR.createErrorType();
			errorType.setErrorDetails(result.get(ResultMapKey.ERROR).toString());
			toReturn.append(ProtocolStatusCode.marshal(errorType));
		}
		
		return toReturn.toString();
	}
  
    
    private String getBody(Map<ResultMapKey, Object> results, OutputFormat format, String details) {
    	String toReturn = null;
    	
		if (!results.containsKey(ResultMapKey.ERROR)) {
			toReturn = (String) results.get(ResultMapKey.DATA);
			if(toReturn == null) {
				ErrorType errorType = ProtocolStatusCode.NO_RESULTS_FOUND.createErrorType();
				errorType.setErrorDetails("No elements found for: " + details);
				toReturn = ProtocolStatusCode.marshal(errorType);
			} 
		} else {
			ErrorType errorType = ProtocolStatusCode.INTERNAL_ERROR.createErrorType();
			errorType.setErrorDetails(results.get(ResultMapKey.ERROR).toString());
			toReturn = ProtocolStatusCode.marshal(errorType);
		}
		
		return toReturn;
	}
}