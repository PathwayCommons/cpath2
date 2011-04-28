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

import java.util.*;

import cpath.service.CPathService.OutputFormat;
import cpath.warehouse.internal.BioDataTypes;
import cpath.warehouse.internal.BioDataTypes.Type;
import cpath.webservice.args.*;
import cpath.webservice.args.binding.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;


/**
 * cPathSquared Help Web Service.
 * 
 * @author rodche
 */
@Controller
@RequestMapping("/help")
public class HelpController {
    private static final Log log = LogFactory.getLog(HelpController.class);
    private static String newline = System.getProperty("line.separator");    
    
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
        binder.registerCustomEditor(Class.class, new BiopaxTypeEditor());
        binder.registerCustomEditor(Cmd.class, new CmdEditor());
    }

	
    @RequestMapping("/commands")
    public String getCommands(Model model) {
    	///Map<String, String> cmds = new HashMap<String, String>();
    	List<String> cmds = new ArrayList<String>();
    	for(Cmd c : Cmd.values()) {
    		cmds.add(c.toString());
    	}
    	model.addAttribute("list", cmds);
    	model.addAttribute("title", "help/commands");
    	return "help";
    }
    
    
    @RequestMapping("/commands/{cmd}")
    public String getCommand(@PathVariable Cmd cmd) {
    	StringBuffer toReturn = new StringBuffer();
    	//TODO
    	return "TODO: describe command " + cmd;
    }
    
    
    @RequestMapping("/commands/{cmd}/")
    public String getCommandArgs(@PathVariable Cmd cmd) {
    	StringBuffer toReturn = new StringBuffer();
    	//TODO
    	return "TODO: describe " + cmd + " parameters...";
    }
    
    
    @RequestMapping("/commands/{cmd}/args/{arg}")
    public String getCommands(@PathVariable Cmd cmd, @PathVariable String arg) {
    	StringBuffer toReturn = new StringBuffer();
    	//TODO
    	return "TODO: for " + cmd + ", describe parameter " + arg;
    }
    
	
	/**
	 * List of formats that web methods return
	 * 
	 * @return
	 */
    @RequestMapping("/formats")
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
    @RequestMapping("/datasources")
    public String getDatasources() {
    	StringBuffer toReturn = new StringBuffer();
    	for(String ds : BioDataTypes.getDataSourceKeys(Type.PATHWAY_DATA)) {
    		toReturn.append(ds).append(newline);
    	}
    	return toReturn.toString();
    }	

}