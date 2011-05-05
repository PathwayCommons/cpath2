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

import cpath.service.CPathService.OutputFormat;
import cpath.service.BioDataTypes;
import cpath.service.BioDataTypes.Type;
import cpath.webservice.args.*;
import cpath.webservice.args.binding.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.level3.Provenance;
import org.biopax.paxtools.model.level3.Xref;
import org.bridgedb.DataSource;
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
    private static final Log log = LogFactory.getLog(HelpController.class);   
    
	public HelpController() {
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
        binder.registerCustomEditor(OrganismDataSource.class, new OrganismDataSourceEditor());
        binder.registerCustomEditor(PathwayDataSource.class, new PathwayDataSourceEditor());
        binder.registerCustomEditor(Class.class, new BiopaxTypeEditor());
    }

	
    @RequestMapping("/")
    public String getHello() {
    	return "redirect:help";
    }
	
    /*
     * Using @ResponseBody with returning a bean
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
    	Help help = new Help();
    	help.setId(cmd.name());
		help.setTitle(cmd.name());
		help.setInfo(cmd.getInfo());
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
    	for(Class<? extends BioPAXElement> t : BiopaxTypeEditor.getTypes()) 
    	{
    		if(BioPAXLevel.L3.getDefaultFactory().getImplClass(t) != null)
    			help.addMember(new Help(t.getSimpleName()));
    	}
    	help.setId("types");
    	help.setTitle("BioPAX classes");
    	help.setInfo("Objects of the following BioPAX L3 " +
    		"(and some additional abstract) " 
    		+ System.getProperty("line.separator") +
    		"class are persisted/indexed in the system " +
    		"(names are case insensitive):");
    	help.setExample("search?type=pathway&q=b*");
    	return help;
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
    	Help help = new Help();
    	help.setTitle(kind.name());
    	help.setId(kind.name());
    	help.setInfo(kind.getFullName());
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
    	for(DataSource ds : BioDataTypes.getDataSources(Type.DATASOURCE)) 
    	{
    		help.addMember(getDatasource(new PathwayDataSource(ds)));
    	}
    	help.setTitle("Pathway Data Sources");
    	help.setInfo("Pathway data sources currently loaded into the system.");
    	return help;
    }
    

    @RequestMapping("/help/datasources/{pds}") 
    public @ResponseBody Help getDatasource(@PathVariable PathwayDataSource pds) 
    {
    	final String newLine = System.getProperty("line.separator");
    	Help help = new Help();
    	DataSource ds = pds.asDataSource();
    	help.setId(ds.getSystemCode());
    	help.setTitle(ds.getFullName());
    	
    	StringBuffer sb = new StringBuffer();
    	// hack (BridgeDb): it has the Provenance stored in ds.organism ;)
    	Provenance pro = ((Provenance)ds.getOrganism());
    	sb.append("Known names: ");	
    	for(String name : pro.getName()) {
    		sb.append(name).append(", ");
    	}
    	sb.append(newLine);
    	sb.append("Xrefs: ");	
    	for(Xref x : pro.getXref()) {
    		sb.append(x + ", "); //x.toString() is called implicitly
    	}
    	sb.append(newLine);
    	sb.append("Comments: ");
    	for(String rem : pro.getComment()) {
    		sb.append(rem).append(newLine);
    	}
    	sb.append("URL: " + ds.getMainUrl());
    	sb.append(newLine);
    	
    	help.setInfo(sb.toString());
    	
    	return help;
    }
    
    
    @RequestMapping("/help/organisms") 
    public @ResponseBody Help getOrganisms() {
    	Help help = new Help();
    	help.setId("organisms");
    	for(DataSource ds : BioDataTypes.getDataSources(Type.ORGANISM)) {
    		help.addMember(getOrganism(new OrganismDataSource(ds)));
    	}
    	help.setTitle("Organisms");
    	help.setInfo("Bio sources currently loaded into the system are");
    	return help;
    }
    

    @RequestMapping("/help/organisms/{o}") 
    public @ResponseBody Help getOrganism(@PathVariable OrganismDataSource o) {
    	Help help = new Help();
    	String taxid = o.asDataSource().getSystemCode();
    	help.setId(taxid); //taxonomy id
    	help.setTitle(o.asDataSource().getFullName());
    	// a hack (BioSource was stored in the o.organism field) -
    	help.setInfo(o.asDataSource().getOrganism().toString()); 
    	//- got the name and Miriam URN (only when data were normalized)

    	return help;
    }
    
    
    @RequestMapping("/help/statistics") 
    public @ResponseBody Help getStatistics() {
    	Help h = new Help("statistics");
    	h.setInfo("TODO: get num. of molecules, pathways, interactions, etc...");
    	// TODO statistics: num. of molecules, pathways, interactions, etc...
    	return h;
    }
}