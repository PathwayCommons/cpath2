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

// imports
import cpath.dao.PaxtoolsDAO;
import cpath.warehouse.CPathWarehouse;

import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.proxy.level3.Level3ElementProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import org.apache.commons.logging.*;

import java.util.*;

@Controller
public class WebserviceController {
    private static final Log log = LogFactory.getLog(WebserviceController.class);

	private PaxtoolsDAO paxtoolsDAO;
	private CPathWarehouse warehouse;

	@Autowired
	public WebserviceController(PaxtoolsDAO paxtoolsDAO, CPathWarehouse warehouse) {
		this.paxtoolsDAO = paxtoolsDAO;
		this.warehouse = warehouse;
	}


	/**
	 * TODO implement; use a "format" enum; write to servlet responce out or use @ResponseBody...
	 * 
	 * @param format
	 * @param id
	 * @param model
	 * @return
	 */
    @RequestMapping("/{format}/{id}")
    public void getElement(@PathVariable("format") String format, @PathVariable("id") Long id, Model model) {
		BioPAXElement element = paxtoolsDAO.getByID(id, true);
		model.addAttribute("element", element);
    }
    
    
    /**
     * TODO search, return the list of URNs (IDs) to the servlet response's output stream...
	 *
     * @return the search-results
     */
    @RequestMapping(value="/search/{clazz}/{query}")
    public void search(@RequestParam("clazz") String clazz, @RequestParam("query") String query, Model model) {
		paxtoolsDAO.search(query, Level3ElementProxy.class);
	}

}