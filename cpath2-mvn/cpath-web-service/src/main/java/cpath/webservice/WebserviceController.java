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
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.proxy.level3.Level3ElementProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.io.*;
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
        binder.registerCustomEditor(BioPAXElement.class, new BiopaxTypeEditor());
    }
	
	
    @RequestMapping("/formats")
    public void getFormats(Writer writer) throws IOException {
    	StringBuffer toReturn = new StringBuffer();
    	for(Format f : Format.values()) {
    		toReturn.append(f.toString().toLowerCase()).append(newline);
    	}
    	writer.write(toReturn.toString());
    }
    
	
    @RequestMapping("/types")
    public void getBiopaxTypes(Writer writer) throws IOException {
    	StringBuffer toReturn = new StringBuffer();
    	for(String type : BiopaxTypeEditor.getTypeByName().keySet()) {
    		toReturn.append(type).append(newline);
    	}
    	writer.write(toReturn.toString());
    }

    
    /*
     * TODO all objects?.. This might be too much to ask :)
     */
    @Deprecated
    @RequestMapping("/elements")
    //@ResponseBody // not required unless we want to use http message to type converters!..
    public void getElements(Writer writer) throws IOException {
    	StringBuffer toReturn = new StringBuffer();
    	for(BioPAXElement e : paxtoolsDAO.getObjects(BioPAXElement.class, false)) {
    		toReturn.append(e.getRDFId()).append(newline);
    	}
    	writer.write(toReturn.toString());
    }
    
    
    @RequestMapping("/elements/{uri}")
    @ResponseBody
    public String getElementById(@PathVariable("uri") String uri) throws IOException {
    	return getElementById(Format.BIOPAX, uri);
    }

    
    @RequestMapping("/format/{format}/elements/{uri}")
    public String getElementById(@PathVariable("format") Format format, 
    		@PathVariable("uri") String uri) {
		BioPAXElement element = paxtoolsDAO.getByID(uri.trim(), true); // (uses HQL saved query)
		// TODO create/use a query format enumeration; process other 'format'
		if(log.isInfoEnabled()) log.info("Query - format:" + format + 
				", urn:" + uri + ", returned:" + element);
		String owl = toOWL(element);
		return owl;
    }

/*    
	@RequestMapping(value="/parentsof/{uri}")
    public void fulltextSearchParent(@PathVariable("uri") String urn, 
    		Writer writer) throws IOException {
		StringBuffer toReturn = new StringBuffer("Not Implemented Yet :)");
		writer.write(toReturn.toString());
	}
    
    
	@RequestMapping(value="/types/{type}/parentsof/{uri}")
    public String fulltextSearchParentOfType(@PathVariable("type") String type, 
    		@PathVariable("uri") String uri, Writer writer) {
		StringBuffer toReturn = new StringBuffer("Not Implemented Yet :)");
		return toReturn.toString(); 
	}
	
	
	@RequestMapping(value="/childrenof/{uri}")
    public String fulltextSearchChild(@PathVariable("uri") String urn, Writer writer) {
		StringBuffer toReturn = new StringBuffer("Not Implemented Yet :)");
		return toReturn.toString(); 
	}
    
	
	@RequestMapping(value="/types/{type}/childrenof/{uri}")
    public String fulltextSearchChildOfType(@PathVariable("type") String type, 
    		@PathVariable("uri") String urn, Writer writer) {
		StringBuffer toReturn = new StringBuffer("Not Implemented Yet :)");
		return toReturn.toString(); 
	}
*/    
    
	@RequestMapping(value="/find/{query}")
    public String fulltextSearch(@PathVariable("query") String query, Writer writer) {
		Collection<? extends Level3ElementProxy> resultSet = 
			paxtoolsDAO.search(query, Level3ElementProxy.class);
		
		StringBuffer toReturn = new StringBuffer();
		for(BioPAXElement el : resultSet) {
			toReturn.append(el.getRDFId()).append(newline);
		}
		
		return toReturn.toString(); 
	}
        

    @RequestMapping(value="/types/{type}/find/{query}")
    public String fulltextSearchForType(@PathVariable("type") String type, 
    		@PathVariable("query") String query, Writer writer) {
    	Class<? extends Level3ElementProxy> classToSearch;
		try {
			classToSearch = (Class<? extends Level3ElementProxy>) Class
				.forName("org.biopax.paxtools.model.level3." + type + "Proxy");
		} catch (ClassNotFoundException e) {
			return "";
		}
		
		Collection<? extends Level3Element> resultSet = paxtoolsDAO.search(query, classToSearch);
		
		StringBuffer toReturn = new StringBuffer();
		for(BioPAXElement el : resultSet) {
			toReturn.append(el.getRDFId()).append(newline);
		}
		
		return toReturn.toString(); 
	}
    
	
	@RequestMapping(value="/graph", method = RequestMethod.POST)
    public String graphQuery(@RequestParam("kind") GraphType kind,  
    		@RequestParam("format") Format format, @RequestParam("source") String source,
    		@RequestParam("dest") String dest, Writer writer) {
		if(log.isInfoEnabled()) log.info("GraphQuery format:" + format + 
				", kind:" + kind + ", source:" + source + ", dest:" + dest);
		StringBuffer toReturn = new StringBuffer("Not Implemented Yet :)" 
				+ "GraphQuery format:" + format + 
				", kind:" + kind + ", source:" + source + ", dest:" + dest);
		return toReturn.toString(); 
	}
	
	
	@RequestMapping(value="/graph", method = RequestMethod.GET)
    public String testForm() {
		return "graph";
	}
	
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