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

import cpath.warehouse.CPathWarehouse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.io.simpleIO.SimpleExporter;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.level3.Level3Element;
import org.biopax.paxtools.model.level3.UtilityClass;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.io.*;

import javax.annotation.PostConstruct;

/**
 * The "Second Query" web controller.
 * 
 * @author rodche
 *
 */
@Controller
@RequestMapping("/warehouse/*")
public class Webservice2Controller {
    private static final Log log = LogFactory.getLog(Webservice2Controller.class);
    private static String newline = System.getProperty("line.separator");
	private CPathWarehouse warehouse;
	private SimpleExporter exporter;	

	//@Autowired
	public Webservice2Controller(CPathWarehouse warehouse, 
			 SimpleExporter exporter) {
		this.warehouse = warehouse;
		this.exporter = exporter;
	}

	@PostConstruct
	void init() {
		warehouse.createIndex();
	}
	
	/**
	 * Customizes request strings conversion to internal types,
	 * e.g., "network of interest" is recognized as GraphType.NETWORK_OF_INTEREST, etc.
	 * 
	 * @param binder
	 */
	@InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Class.class, new BiopaxTypeEditor());
    }
	
      
	/**
	 * Gets utility element by ID.
	 * 
	 * TODO modify - to return BioPAX OWL representation of the element...
	 * 
	 * @param type concrete BioPAX UtilityClass (ProteinReference, SmallMoleculeReference, CV types)
	 * @param urn Miriam standard version of the data type identifier (e.g., urm:miriam:obo.go:GO%3A00123)
	 * @return
	 */
    @RequestMapping(value="/{type}/get")
    @ResponseBody
    public String getElementsOfType(@PathVariable("type") Class<? extends UtilityClass> type,
    		@RequestParam("urn") String urn) {
    	if(log.isInfoEnabled()) log.info("Warehouse query for type:" + type.getSimpleName() 
    			+ ", urn:" + urn);
    	StringBuffer toReturn = new StringBuffer();
		if (UtilityClass.class.isAssignableFrom(type)) {
			UtilityClass el = warehouse.getObject(urn, type);
			if(el != null) {
				toReturn.append(el.getRDFId()).append(newline);
			}
		}
    	return toReturn.toString();
    }
    
    
	@RequestMapping(value="/find/{query}")
	@ResponseBody
    public String fulltextSearch(@PathVariable("query") String query) {
		return fulltextSearchForType(Level3Element.class, query);
	}
        

    @RequestMapping(value="/types/{type}/find/{query}")
    @ResponseBody
    public String fulltextSearchForType(@PathVariable("type") Class<? extends BioPAXElement> type, 
    		@PathVariable("query") String query) {	
    	if(log.isInfoEnabled()) log.info("Warehouse fulltext Search for type:" 
				+ type.getCanonicalName() + ", query:" + query);
    	
    	StringBuffer toReturn = new StringBuffer();
    	
    	/*
    	 * TODO add search capability to Warehouse
		List<BioPAXElement> results = (List<BioPAXElement>) warehouse.search(query, type);
		for(BioPAXElement e : results) {
			toReturn.append(e.getRDFId()).append(newline);
		}
		*/
		
		return toReturn.toString(); 
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
	
}