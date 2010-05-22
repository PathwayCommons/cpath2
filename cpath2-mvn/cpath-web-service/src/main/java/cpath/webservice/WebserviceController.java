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
import org.biopax.miriam.MiriamLink;
import org.biopax.paxtools.impl.BioPAXElementImpl;
import org.biopax.paxtools.io.simpleIO.SimpleExporter;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.bridgedb.DataSource;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.*;

import javax.annotation.PostConstruct;

@Controller
public class WebserviceController {
    private static final Log log = LogFactory.getLog(WebserviceController.class);
    private static String newline = System.getProperty("line.separator");
	private PaxtoolsDAO pcDAO;
	private SimpleExporter exporter;

    
    @PostConstruct
    void init() {
    	// re-build Lucene index (TODO is this required?)
    	pcDAO.createIndex();
    	
    	/* TODO consider moving the following to Warehouse altogether 
    	 * - create BioDataTypes class with the only static method;
    	 * - init() would register with org.bridgedb.DataSource all the 
    	 * data sources found in Warehouse's Metadata, MIRIAM, 
    	 * plus - custom data types (cpath1 legacy, e.g., CELL_MAP);
    	 */
    	
    	// TODO register all the data providers (from Warehouse's Metadata)
    	
    	// register all MIRIAM data types in BridgeDB's DataSource
    	for(String name : MiriamLink.getDataTypesName()) {
    		// register all synonyms (incl. the name)
    		for(String s : MiriamLink.getNames(name)) {
    			DataSource.register(s, name)
    			  .urnBase(MiriamLink.getDataTypeURI(name));
    		}
    	}
    	
    	// manually register legacy (cpath) data source names
    	DataSource.register("BIOGRID", "BioGRID");
    	DataSource.register("CELL_MAP", "Cancer Cell Map"); // add to Miriam
    	DataSource.register("HPRD", "HPRD"); // add to Miriam
    	DataSource.register("HUMANCYC", "HumanCyc");
    	DataSource.register("IMID", "IMID");
    	DataSource.register("INTACT", "IntAct");
    	DataSource.register("MINT", "MINT");
    	DataSource.register("NCI_NATURE", "NCI / Nature Pathway Interaction Database");
    	DataSource.register("REACTOME", "Reactome");
    }
    

	//@Autowired
	public WebserviceController(PaxtoolsDAO mainDAO, SimpleExporter exporter) {
		this.pcDAO = mainDAO;
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
        binder.registerCustomEditor(OutputFormat.class, new OutputFormatEditor());
        binder.registerCustomEditor(GraphType.class, new GraphTypeEditor());
        binder.registerCustomEditor(Class.class, new BiopaxTypeEditor());
        binder.registerCustomEditor(DataSource.class, new DataSourceEditor());
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
    		toReturn.append(f.toString().toLowerCase()).append(newline);
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
    	StringBuffer toReturn = new StringBuffer();
    	/* getObjects with eager=false, statless=false is ok for RDFIds only...
    	 * - no need to detach elements from the DAA session
    	 */
    	Set<? extends BioPAXElement> results = pcDAO.getElements(type, false); 
    	for(BioPAXElement e : results)
    	{
    		toReturn.append(e.getRDFId()).append(newline);
    	}
    	return toReturn.toString();
    }
    
     
    //=== Most critical web methods that get one element by ID (URI) ===//

    
    @RequestMapping(value="/elements")
    @ResponseBody
    public String elementById(@RequestParam("uri") String uri) {
    	if(log.isInfoEnabled()) log.info("POST Query /elements");
    	return elementById(OutputFormat.BIOPAX, uri);
    }

    
    @RequestMapping(value="/format/{format}/elements")
    @ResponseBody
    public String elementById(@PathVariable("format") OutputFormat format, 
    		@RequestParam("uri") String uri) 
    {
    	BioPAXElement element = pcDAO.getElement(uri, true); 
    	element = pcDAO.detach(element);

		if(log.isInfoEnabled()) log.info("Query - format:" + format + 
				", urn:" + uri + ", returned:" + element);

		String owl = toOWL(element);		
		return owl;
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
    	
    	// do search
		List<BioPAXElement> results = (List<BioPAXElement>) pcDAO.search(query, type);
		StringBuffer toReturn = new StringBuffer();
		for(BioPAXElement e : results) {
			toReturn.append(e.getRDFId()).append(newline);
		}
		
		return toReturn.toString(); 
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