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

import java.io.IOException;

import cpath.service.jaxb.*;
import cpath.service.CPathService;
import cpath.service.Cmd;
import cpath.service.CmdArgs;
import cpath.service.GraphType;
import cpath.service.OutputFormat;
import cpath.service.Status;
import cpath.webservice.args.binding.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.controller.EditorMap;
import org.biopax.paxtools.controller.PropertyEditor;
import org.biopax.paxtools.controller.SimpleEditorMap;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.query.algorithm.Direction;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;


/**
 * cPathSquared Help Web Service.
 * 
 * Can return XML (default) or
 * JSON (on request) serialized
 * {@link Help} bean!
 * 
 * @author rodche
 */
@Controller
public class HelpController {
    private static final Log LOG = LogFactory.getLog(HelpController.class);   
    
    private CPathService service; // main PC db access
    
	public HelpController(CPathService service) {
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
        binder.registerCustomEditor(Cmd.class, new CmdEditor());
        binder.registerCustomEditor(CmdArgs.class, new CmdArgsEditor());
        binder.registerCustomEditor(Direction.class, new DirectionEditor());
        binder.registerCustomEditor(Class.class, new BiopaxTypeEditor());
    }

	
	/**
	* This is for reporting an error "BAD COMMAND"
	* for everything except for known cpath2 web service
	* commands (known commands with parameters are mapped 
	* to more specific controller methods in this class; see below)
	* 
	* @param cmd
	* 
	* @return 
	 * @throws IOException 
	*/
	@RequestMapping("/{cmd}")
	public @ResponseBody ErrorResponse illegalCommand(@PathVariable String cmd) {
		return Status.BAD_COMMAND.errorResponse("Unknown command: " + cmd);
	}
	
	
    @RequestMapping("/")
    public String getHello() {
    	return "redirect:help";
    }
	
	 
    /*
     * Using @Response with returning a bean
     * makes it auto-generate xml or json, 
     * depending on the client's http request
     * (no extra coding required!)
     */
    @RequestMapping("/help")
    public @ResponseBody Help getHelp() {
    	Help help = new Help();
    	help.setId("help");
    	help.setTitle("Help");
    	help.setInfo("Welcome to cPath2 Webservice Help");
    	help.setExample("help/commands");
    	// Help tree's five main branches:
    	help.addMember(getCommands()); // sub-tree for commands and their args info
    	help.addMember(getDatasources());
       	help.addMember(getOrganisms());
    	help.addMember(getFormats());
    	help.addMember(getGraphTypes());
    	help.addMember(getBiopaxTypes());
    	return help;
    }

    
    @RequestMapping("/help/commands")
    public @ResponseBody Help getCommands() {
    	Help help = new Help();
    	for(Cmd c : Cmd.values()) {
    		help.addMember(getCommand(c));
    	}
    	help.setId("commands");
    	help.setInfo("cPath2 BioPAX L3 web service supports "
    		+ Cmd.values().length + " commands");
    	help.setTitle("cPath2 Webservice Commands");
    	help.setExample("search?q=brca*&type=protein");
    	return help;
    }    
 
    
    @RequestMapping("/help/commands/{cmd}")
    public @ResponseBody Help getCommand(@PathVariable Cmd cmd) {
    	if(cmd == null) return getCommands();
    	Help help = new Help();
    	help.setId(cmd.name());
		help.setTitle(cmd.name());
		help.setInfo(cmd.getInfo());
        help.setExample(cmd.getExample());
        help.setOutput(cmd.getOutput());
		for(CmdArgs a: cmd.getArgs()) {
			Help ah = new Help(a.name());
			ah.setTitle(a.name());
			ah.setInfo(a.getInfo());
			help.addMember(ah);
		}
    	return help;
    }
    
    
	/**
	 * List of formats that web methods return
	 * 
	 * @return
	 */
    @RequestMapping("/help/formats")
    public @ResponseBody Help getFormats() {
    	Help help = new Help();
    	for(OutputFormat f : OutputFormat.values()) {
    		help.addMember(getFormat(f));
    	}
    	help.setId("formats");
    	help.setTitle("Output Formats");
    	help.setInfo("cPath2 can convert BioPAX to several text formats");
    	help.setExample("help/formats/binary_sif");
    	return help;
    }

    
    @RequestMapping("/help/formats/{fmt}")
    public @ResponseBody Help getFormat(@PathVariable OutputFormat fmt) {
    	if(fmt == null) return getFormats();
    	Help help = new Help();
    	help.setId(fmt.name());
    	help.setTitle(fmt.name());
    	help.setInfo(fmt.getInfo());
    	return help;
    }
    
	/**
	 * List of BioPAX L3 Classes
	 * 
	 * @return
	 */
    @RequestMapping("/help/types")
    public @ResponseBody Help getBiopaxTypes() {
    	Help help = new Help();
    	
    	for(Class<? extends BioPAXElement> t : 
    		SimpleEditorMap.L3.getKnownSubClassesOf(BioPAXElement.class)) 
    	{
    		if(BioPAXLevel.L3.getDefaultFactory().getImplClass(t) != null)
    			help.addMember(new Help(t.getSimpleName()));
    	}
    	help.setId("types");
    	help.setTitle("BioPAX classes");
    	help.setInfo("Objects of the following BioPAX L3 class " +
    		"(and some abstract ones) " 
    		+ System.getProperty("line.separator") +
    		"are persisted/indexed/searchable in the system " +
    		"(names are case insensitive):");
    	help.setExample("search?type=pathway&q=b*");
    	return help;
    }
    
    
    @RequestMapping("/help/types/{type}") 
    public @ResponseBody Help getBiopaxType(@PathVariable Class<? extends BioPAXElement> type) {
    	if(type == null) return getBiopaxTypes();
    	
    	Help h = new Help(type.getSimpleName());
    	h.setTitle(type.getSimpleName());
    	h.setInfo("See: biopax.org, http://www.biopax.org/webprotege");
    	
    	return h;
    }

    
    @RequestMapping("/help/types/{type}/properties") 
    public @ResponseBody Help getBiopaxTypeProperties(@PathVariable Class<? extends BioPAXElement> type) {
    	final String id = type.getSimpleName() + " properties";
    	Help h = new Help(id);
    	h.setTitle(id);
    	h.setInfo("BioPAX properties " +
    		"for class: " + type.getSimpleName());
    	
    	EditorMap em = SimpleEditorMap.get(BioPAXLevel.L3);
    	for(PropertyEditor e : em.getEditorsOf(type)) 
    		h.addMember(new Help(e.toString()));
    	
    	return h;
    }
    
    
    @RequestMapping("/help/types/properties") 
    public @ResponseBody Help getBiopaxTypesProperties() {
    	Help h = new Help("properties");
    	h.setTitle("BioPAX Properites");
    	h.setInfo("The list of all BioPAX properties");
    	 	
    	for(Class<? extends BioPAXElement> t : 
    		SimpleEditorMap.L3.getKnownSubClassesOf(BioPAXElement.class)) 
    	{
    		if(BioPAXLevel.L3.getDefaultFactory().getImplClass(t) != null) {
    			for(Help th : getBiopaxTypeProperties(t).getMembers()) {
    				h.addMember(th);
    			}
    		}
    	}
    	
    	return h;
    }
    
    @RequestMapping("/help/types/{type}/inverse_properties") 
    public @ResponseBody Help getBiopaxTypeInverseProperties(@PathVariable Class<? extends BioPAXElement> type) {
    	final String id = type.getSimpleName() + " inverse_properties";
    	Help h = new Help(id);
    	h.setTitle(id);
    	h.setInfo("Paxtools inverse properties " +
    		"for class: " + type.getSimpleName());
    	
    	EditorMap em = SimpleEditorMap.get(BioPAXLevel.L3);
    	for(PropertyEditor e : em.getInverseEditorsOf(type)) 
    		h.addMember(new Help(e.toString()));
    	
    	return h;
    }
    
    
    @RequestMapping("/help/types/inverse_properties") 
    public @ResponseBody Help getBiopaxTypesInverseProperties() {
    	Help h = new Help("inverse_properties");
    	h.setTitle("Paxtools inverse properites");
    	h.setInfo("The list of all inverse (Paxtools) properties");
    	 	
    	for(Class<? extends BioPAXElement> t : 
    		SimpleEditorMap.L3.getKnownSubClassesOf(BioPAXElement.class)) 
    	{
    		if(BioPAXLevel.L3.getDefaultFactory().getImplClass(t) != null) {
    			for(Help th : getBiopaxTypeInverseProperties(t).getMembers()) {
    				h.addMember(th);
    			}
    		}
    	}
    	
    	return h;
    }
    
	/**
	 * List of graph query types.
	 * 
	 * @return
	 */
    @RequestMapping("/help/kinds")
    public @ResponseBody Help getGraphTypes() {
    	Help help = new Help();
    	for(GraphType type : GraphType.values()) {
    		help.addMember(getGraphType(type));
    	}
    	help.setId("kinds");
    	help.setTitle("Advanced Graph Query Types");
    	help.setInfo("cPath2 has the following built-in algorithms:");
    	help.setExample("help/kinds/neighborhood");
    	return help;
    }

    
    @RequestMapping("/help/kinds/{kind}")
    public @ResponseBody Help getGraphType(@PathVariable GraphType kind) {
    	if(kind == null) return getGraphTypes();
    	Help help = new Help();
    	help.setTitle(kind.name());
    	help.setId(kind.name());
    	help.setInfo(kind.getDescription());
    	return help;
    }

    
	/**
	 * List of graph directions.
	 *
	 * @return
	 */
    @RequestMapping("/help/directions")
    public @ResponseBody Help getDirectionTypes() {
    	Help help = new Help();
    	for(Direction direction : Direction.values()) {
    		help.addMember(getDirectionType(direction));
    	}
    	help.setId("directions");
    	help.setTitle("Graph Query Traversal Directions");
    	help.setInfo("Following are possible query directions:");
    	help.setExample("help/directions/downstream");
    	return help;
    }


    @RequestMapping("/help/directions/{direction}")
    public @ResponseBody Help getDirectionType(@PathVariable Direction direction) {
    	if(direction == null) return getDirectionTypes();
    	Help help = new Help();
    	help.setTitle(direction.name());
    	help.setId(direction.name());
    	help.setInfo(direction.getDescription());
    	return help;
    }


	/**
	 * List of loaded data sources.
	 * 
	 * @return
	 */
    @RequestMapping("/help/datasources") 
    public @ResponseBody Help getDatasources() {
    	Help help = new Help();
    	help.setId("datasources");

    	for(SearchHit ds : service.dataSources().getSearchHit()) 
    	{
    		Help hm = new Help(ds.getUri());
    		hm.setTitle(ds.getName());
    		hm.setInfo(ds.getDataSource().toString()); // using a hack to get other names
    		help.addMember(hm);
    	}
    	help.setTitle("Pathway Data Sources");
    	help.setInfo("Bio interactions data were imported into the system. " +
    			"These values (standard name, URI) are recommended to use with the " +
    			"full-text search command (e.g., by adding '&datasource=...' " +
    			"filter parameters). " + 
    			"Other (original pathway data provider's) BioPAX Provenance " +
    			"objects there can be used as well, but may be associated with " +
    			"only a sub-set of entities loaded from given data source.");
    	return help;
    }
       
    
    @RequestMapping("/help/organisms") 
    public @ResponseBody Help getOrganisms() {
    	Help help = new Help();
    	help.setId("organisms");
    	
    	for(SearchHit bs : service.bioSources().getSearchHit()) {
    		Help hm = new Help(bs.getUri());
    		hm.setTitle(bs.getName());
    		hm.setInfo(bs.getOrganism().toString()); // using a hack to list other names
    		help.addMember(hm);
    	}
    	help.setTitle("Organisms");
    	help.setInfo("All organisms (BioSource) referenced in this system " +
    		"(incl. those occur in other organism's pathways or in annotations)");
    	return help;
    }


}