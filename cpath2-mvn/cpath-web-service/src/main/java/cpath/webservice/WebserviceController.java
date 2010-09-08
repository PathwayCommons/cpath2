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

import cpath.dao.CPathService;
import cpath.dao.PaxtoolsDAO;
import cpath.dao.CPathService.OutputFormat;
import cpath.dao.CPathService.ResultMapKey;
import cpath.dao.internal.CPathServiceImpl;
import cpath.warehouse.CvRepository;
import cpath.warehouse.internal.BioDataTypes;
import cpath.warehouse.internal.BioDataTypes.Type;
import cpath.webservice.args.*;
import cpath.webservice.args.binding.*;
import cpath.webservice.jaxb.ErrorType;
import cpath.webservice.jaxb.ExtendedRecordType;
import cpath.webservice.jaxb.SearchResponseType;
import cpath.webservice.jaxb.SummaryResponseType;
import cpath.webservice.validation.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.level3.Named;
import org.biopax.paxtools.model.level3.UtilityClass;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.io.StringWriter;
import java.util.*;

import javax.validation.constraints.NotNull;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

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
    	
    	Map<ResultMapKey, Object> result = service.element(uri, format);
    	
    	String body = getBody(result, format, uri);
    	
		return body;
    }
	
	
    // Fulltext Search
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
    	Map<ResultMapKey,Object> results = service.list(query, type, false);
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
    		//@RequestBody MultiValueMap<String,String> map // it's easier to initialize the ProtocolRequest below...
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
		
		try {
			// build the ProtocolRequest from the Map
			protocol = new ProtocolRequest(argsMap);
			// validate by ProtocolValidator
			ProtocolValidator protocolValidator = new ProtocolValidator(protocol);
			protocolValidator.validate();
		} catch (ProtocolException e) {
			ErrorType errorType = e.getStatusCode().createErrorType();
			// set the error details
			errorType.setErrorDetails(e.getMessage());
			// build the xml string
			return ProtocolStatusCode.marshal(errorType);
		}
		
		

		// TODO execute query and get results here
		if(protocol.getCommand() == Cmd.SEARCH) {
			//return "forward:search.html";
			// output format is always the 'xml'
			// build a SearchResponseType (from xsd), marshal
			if(log.isDebugEnabled()) log.debug("Legacy (cpath) Fulltext Search:" 
					+ ", query:" + protocol.getQuery());
	    	Map<ResultMapKey,Object> results = service.list(protocol.getQuery(), null, false);
			if(results.containsKey(ResultMapKey.ERROR)) {
				return internalError(results.get(ResultMapKey.ERROR).toString());
			}
	    	
			Collection<String> idList = (Collection<String>) results.get(ResultMapKey.DATA);
			SearchResponseType searchResponse = new SearchResponseType();
			searchResponse.setTotalNumHits(Long.valueOf(idList.size()));
			List<ExtendedRecordType> hits = searchResponse.getSearchHit();
			for (String id : idList) {
				Map<ResultMapKey, Object> result = service.element(id, OutputFormat.BIOPAX);
				if(result.containsKey(ResultMapKey.ERROR)) {
					return internalError(result.get(ResultMapKey.ERROR).toString());
				}
				BioPAXElement value = (BioPAXElement) result.get(ResultMapKey.ELEMENT);
				
				ExtendedRecordType rec = new ExtendedRecordType();
				rec.setPrimaryId(id);
				if(value instanceof Named)
					rec.setName(((Named)value).getName().toString());
				rec.setEntityType(value.getModelInterface().getSimpleName());
				hits.add(rec);
				// TODO set all fields...
			}
			toReturn = marshalSearchResponce(searchResponse);
			//toReturn = idList.toString(); //TODO return xml
		} else if(protocol.getCommand() == Cmd.GET_RECORD_BY_CPATH_ID) {
			//return "forward:get";
			return internalError("Not Implemented Yet: legacy GET_RECORD_BY_CPATH_ID");
		} else if(protocol.getCommand() == Cmd.GET_BY_KEYWORD) {
			// probably, is the same as "search"
			return internalError("Not Implemented Yet: legacy GET_BY_KEYWORD");
		} else if(protocol.getCommand() == Cmd.GET_PATHWAYS) {
			return internalError("Not Implemented Yet: legacy GET_PATHWAYS");
		} else if(protocol.getCommand() == Cmd.GET_NEIGHBORS) {
			return internalError("Not Implemented Yet: legacy GET_NEIGHBORS");
		} else if(protocol.getCommand() == Cmd.GET_PARENTS) {
			//TODO implement "get_parents" or give up...
			// build a SummaryResponseType (from xsd), marshal
			return internalError("Not Implemented Yet: legacy GET_PARENTS");
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
				ErrorType errorType = ProtocolStatusCode.NO_RESULTS_FOUND.createErrorType();
				errorType.setErrorDetails("No elements found for: " + details);
				toReturn.append(ProtocolStatusCode.marshal(errorType));
			} else {
				for (String s : dataSet) {
					toReturn.append(s).append(newline);
				}
			}
		} else {
			toReturn.append(internalError(result.get(ResultMapKey.ERROR).toString()));		
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
    
	
	/**
	 * @param string
	 * @return
	 */
	private String internalError(String string) {
		ErrorType errorType = ProtocolStatusCode.INTERNAL_ERROR
			.createErrorType();
		errorType.setErrorDetails(string);
		return ProtocolStatusCode.marshal(errorType);
	}

	
	static String marshalSearchResponce(SearchResponseType obj) {
		StringWriter writer = new StringWriter();
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance("cpath.webservice.jaxb");
			Marshaller ma = jaxbContext.createMarshaller();
			ma.setProperty("jaxb.formatted.output", true);
			ma.marshal(
			new JAXBElement<SearchResponseType>(new QName("","search_response"), 
					SearchResponseType.class, obj), writer);
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
		return writer.toString();
	}
	
	static String marshalSummaryResponce(SummaryResponseType obj) {
		StringWriter writer = new StringWriter();
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance("cpath.webservice.jaxb");
			Marshaller ma = jaxbContext.createMarshaller();
			ma.setProperty("jaxb.formatted.output", true);
			ma.marshal(
			new JAXBElement<SummaryResponseType>(new QName("","summary_response"), 
					SummaryResponseType.class, obj), writer);
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
		return writer.toString();
	}
}