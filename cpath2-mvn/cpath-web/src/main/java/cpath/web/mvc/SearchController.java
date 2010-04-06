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
package cpath.web.mvc;

// imports
import cpath.dao.PaxtoolsDAO;
import org.biopax.paxtools.proxy.level3.PathwayProxy;
import org.biopax.paxtools.proxy.level3.ProteinProxy;
import org.biopax.paxtools.proxy.level3.InteractionProxy;
import org.biopax.paxtools.proxy.level3.BioPAXElementProxy;
import org.biopax.paxtools.proxy.level3.PhysicalEntityProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
//import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import org.apache.commons.logging.*;

import java.util.*;

/**
 * Search/Search Results "Controller" (in MVC terms)
 */
@Controller
@RequestMapping("/search")
public class SearchController {
    private static final Log log = LogFactory.getLog(SearchController.class);

	// inject paxtools dao
	private PaxtoolsDAO paxtoolsDAO;

	// the set of biopax element - names to display on page
	private final Set<String> pathwayNames;
	private final Set<String> interactionNames;
	private final Set<String> proteinNames;

	@Autowired
	public SearchController(PaxtoolsDAO paxtoolsDAO) {
		this.paxtoolsDAO = paxtoolsDAO;
		pathwayNames = new HashSet<String>();
		proteinNames = new HashSet<String>();
		interactionNames = new HashSet<String>();
	}


	// simply shows form
	@RequestMapping(method = RequestMethod.GET)
    public void newRequest() {
    }
	
    /**
     * The default event handler that displays a list of Search Results.
	 *
	 * TODO: display a form instead?
	 *
     * @return a forward to the listBy() method of this class
     */
    @RequestMapping(method = RequestMethod.POST)
    public ModelAndView listElements() {
		ModelMap modelMap = processSearch(paxtoolsDAO.getObjects(BioPAXElementProxy.class, true));
        return new ModelAndView("search-results", modelMap);
    }

    /**
     * The event handler for searching.
	 *
     * @return the search-results
     */
    @RequestMapping(method= RequestMethod.POST)
    public ModelAndView search(@RequestParam String queryString) {
		ModelMap modelMap = processSearch(paxtoolsDAO.search(queryString, BioPAXElementProxy.class));
		return new ModelAndView("search-results", modelMap);
	}

	/**
	 * Given a set of biopax elements, partitions into pathway, interaction, protein names.
	 *
	 * @param bpes Set<BioPAXElementProxy>
	 */
	private ModelMap processSearch(Set<BioPAXElementProxy> bpes) {
		pathwayNames.clear();
		interactionNames.clear();
		proteinNames.clear();
		ModelMap map = new ModelMap();
		
		for (BioPAXElementProxy bpe : bpes) {
			// limit display to Interaction or PhysicalEntity
			if (bpe instanceof InteractionProxy) {
				interactionNames.add(((InteractionProxy)bpe).getDisplayName());
			}
			else if (bpe instanceof PhysicalEntityProxy) {
				if (bpe instanceof PathwayProxy) {
					pathwayNames.add(((ProteinProxy)bpe).getDisplayName());
				}
				else if (bpe instanceof ProteinProxy) {
					proteinNames.add(((ProteinProxy)bpe).getDisplayName());
				}
			}
		}
		
		map.put("pathwayNames", pathwayNames);
		map.put("proteinNames",proteinNames);
		map.put("interactionNames",interactionNames);
		
		return map;
	}
}