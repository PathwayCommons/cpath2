// $Id$
//------------------------------------------------------------------------------
/** Copyright (c) 2009 Memorial Sloan-Kettering Cancer Center.
 **
 ** This library is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** any later version.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** Memorial Sloan-Kettering Cancer Center
 ** has no obligations to provide maintenance, support,
 ** updates, enhancements or modifications.  In no event shall
 ** Memorial Sloan-Kettering Cancer Center
 ** be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** Memorial Sloan-Kettering Cancer Center
 ** has been advised of the possibility of such damage.  See
 ** the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this library; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 **/
package cpath.webservice;

import cpath.dao.PaxtoolsDAO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.io.BioPAXIOHandler;
import org.biopax.paxtools.io.simpleIO.SimpleExporter;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.URLDecoder;
import java.util.*;

@Controller
public class WebserviceController {
    private static final Log log = LogFactory.getLog(WebserviceController.class);
    private static String newline = System.getProperty("line.separator");
	private PaxtoolsDAO paxtoolsDAO;
	//private CPathWarehouse warehouse; // for graph queries, "second query", and hierarchical CV queries...
	//private PaxtoolsDAO proteinsDAO;
	//private PaxtoolsDAO moleculesDAO;
	private SimpleExporter exporter;

    // TODO move Format definition outta here?..
    public enum Format {
    	BIOPAX,
    	SIF,
    	SBML,
    	GSEA,
    	GENESET,
    	TSV,
    	IMAGE,
    	;
    	
    	public static Format parseFormat(String value) {
    		for(Format v : Format.values()) {
    			if(value.equalsIgnoreCase(v.toString())) {
    				return v;
    			}
    		}
    		return null;
    	}
    }
    
    // TODO move GraphType definition outta here?..
    public enum GraphType {
    	NEIGHBORHOOD("neighborhood"),
    	COMMON_UPSTREAM("common upstream"),
    	COMMON_DOWNSTREAM("common downstream"),
    	COMMON_TARGET("common target"),
    	NETWORK_OF_INTEREST("network of interest"),
    	K_SHORTEST_PATH("k-shortest path"),
    	;
    	
    	private String value;

		private GraphType(String value) {
			this.value = value;
		}
		
		@Override
		public String toString() {
			return value;
		}
		
    	public static GraphType parseGraphType(String value) {
    		for(GraphType v : GraphType.values()) {
    			if(value.equalsIgnoreCase(v.toString())) {
    				return v;
    			}
    		}
    		return null;
    	}
    }
	
	/**
	 * Constructor
	 * 
	 * @param paxtoolsDAO
	 * @param reader
	 * @param exporter
	 */
	@Autowired
	public WebserviceController(PaxtoolsDAO paxtoolsDAO, //CPathWarehouse warehouse,
			BioPAXIOHandler reader, SimpleExporter exporter) {
		this.paxtoolsDAO = paxtoolsDAO;
		//this.warehouse = warehouse;
		this.exporter = exporter;
	}

	
	/**
	 * Customizes request strings conversion to internal types,
	 * e.g., "network of interest" is recognized as GraphType.NETWORK_OF_INTEREST, etc.
	 * 
	 * @param binder
	 */
	@InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Format.class, new FormatEditor());
        binder.registerCustomEditor(GraphType.class, new GraphTypeEditor());
        binder.registerCustomEditor(Class.class, new BiopaxTypeEditor());
    }
	
	
    @RequestMapping("/formats")
    @ResponseBody
    public String getFormats() {
    	StringBuffer toReturn = new StringBuffer();
    	for(Format f : Format.values()) {
    		toReturn.append(f.toString().toLowerCase()).append(newline);
    	}
    	return toReturn.toString();
    }
    
	
    @RequestMapping("/types")
    @ResponseBody
    public String getBiopaxTypes() {
    	StringBuffer toReturn = new StringBuffer();
    	for(String type : BiopaxTypeEditor.getTypesByName().keySet()) {
    		toReturn.append(type).append(newline);
    	}
    	return toReturn.toString();
    }

    
    /*
     * TODO all objects?.. This might be too much to ask :)
     */
    @Deprecated
    @RequestMapping(value="/elements/all", method=RequestMethod.GET)
    @ResponseBody
    public String getElements() throws IOException {
    	return getElementsOfType(BioPAXElement.class);
    }

    
    @Deprecated
    @RequestMapping(value="/types/{type}/elements", method=RequestMethod.GET)
    @ResponseBody
    public String getElementsOfType(@PathVariable("type") Class<? extends BioPAXElement> type) {
    	StringBuffer toReturn = new StringBuffer();
    	Set<? extends BioPAXElement> results = paxtoolsDAO.getObjects(type, false, false);
    	for(BioPAXElement e : results)
    	{
    		toReturn.append(e.getRDFId()).append(newline);
    	}
    	return toReturn.toString();
    }
    
    
    @RequestMapping(value="/elements", method = RequestMethod.POST)
    @ResponseBody
    public String postElementById(@RequestParam("uri") String uri) {
    	if(log.isInfoEnabled()) log.info("POST Query /elements");
    	return elementById(Format.BIOPAX, uri);
    }

    
    @RequestMapping(value="/elements", method = RequestMethod.GET)
    @ResponseBody
    public String getElementById(@RequestParam("id") String id) {
    	if(log.isInfoEnabled()) log.info("GET Query /elements?id=" + id);
    	return elementById(Format.BIOPAX, id);
    }
    
    
    @Transactional
    @RequestMapping(value="/format/{format}/elements", method= RequestMethod.POST)
    @ResponseBody
    public String elementById(@PathVariable("format") Format format, 
    		@RequestParam("uri") String uri) 
    {
    	BioPAXElement element = paxtoolsDAO.getByID(uri, false, false);
		if(log.isInfoEnabled()) log.info("Query - format:" + format + 
				", urn:" + uri + ", returned:" + element);
		/*
		 * using paxtoolsDAO.getByID(uri, true, true) above 
		 * causes org.hibernate.LazyInitializationException: 
		 *  failed to lazily initialize a collection of role: org.biopax.paxtools.proxy.level3.PathwayProxy.pathwayComponent, 
		 *  no session or session was closed
		String owl = toOWL(element);
		*/
		// TODO how to get complete BioPAX element (or its serialization) from the DAO...
		String owl = element.getRDFId();
		
		return owl;
    }
    
    
	@RequestMapping(value="/find/{query}")
	@ResponseBody
    public String fulltextSearch(@PathVariable("query") String query) {
		return fulltextSearchForType(BioPAXElement.class, query);
	}
        

    @RequestMapping(value="/types/{type}/find/{query}")
    @ResponseBody
    public String fulltextSearchForType(@PathVariable("type") Class<? extends BioPAXElement> type, 
    		@PathVariable("query") String query) {	
    	
    	if(log.isInfoEnabled()) log.info("Fulltext Search for type:" 
				+ type.getCanonicalName() + ", query:" + query);
    	
		List<BioPAXElement> results = (List<BioPAXElement>) paxtoolsDAO.search(query, type);
		StringBuffer toReturn = new StringBuffer();
		for(BioPAXElement e : results) {
			toReturn.append(e.getRDFId()).append(newline);
		}
		
		return toReturn.toString(); 
	}
    
	
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
	
	
	@ExceptionHandler
	@RequestMapping(value="/graph", method = RequestMethod.GET)
    public void testForm() {}
	
	
	private String toOWL(BioPAXElement element) {
		if(element == null) return "NOTFOUND"; // temporary
		
		StringWriter writer = new StringWriter();
		try {
			exporter.writeObject(writer, element);
		} catch (IOException e) {
			log.error(e);
		}
		return writer.toString();
	}
	
	
	private String toOWL(Model model) {
		if(model == null) return null;
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			exporter.convertToOWL(model, out);
		} catch (IOException e) {
			log.error(e);
		}
		return out.toString();
	}

}