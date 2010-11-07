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

import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.level3.Interaction;
import org.biopax.paxtools.model.level3.Named;
import org.biopax.paxtools.model.level3.Pathway;
import org.biopax.paxtools.model.level3.Protein;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import org.apache.commons.logging.*;

import java.util.*;

/**
 * Search/Search Results "Controller" (in MVC terms)
 */
@Controller
public class SearchController {
    private static final Log log = LogFactory.getLog(SearchController.class);

	private final Set<String> pathwayNames;
	private final Set<String> interactionNames;
	private final Set<String> proteinNames;
	private final Set<String> otherNames;

	@Autowired
	public SearchController() {		
		pathwayNames = new HashSet<String>();
		proteinNames = new HashSet<String>();
		interactionNames = new HashSet<String>();
		otherNames = new HashSet<String>();
	}



	@RequestMapping(value="/graph", method = RequestMethod.GET)
    public void graphForm(){
	}
	
	
	// shows form
	@RequestMapping(value="/search", method = RequestMethod.GET)
    public void showForm() {
    }
	

	@RequestMapping("/home")
    public String listAllElements(Model model) {
        return "search-results";
    }

    
    @RequestMapping(value="/search", method= RequestMethod.POST)
    public String search(@RequestParam("queryString") String queryString, Model model) {
		// TODO process
		return "search-results";
	}

	/**
	 * Given a set of biopax elements, partitions into pathway, interaction, protein names.
	 *
	 * @param bpes Set<BioPAXElementProxy>
	 */
	private void processSearch(Set<? extends BioPAXElement> bpes, Model model) {
		pathwayNames.clear();
		interactionNames.clear();
		proteinNames.clear();
		otherNames.clear();
		
		for (BioPAXElement bpe : bpes) {
			// limit display to Interaction or PhysicalEntity
			if (bpe instanceof Interaction) {
				interactionNames.add(((Interaction)bpe).getDisplayName());
			} else if (bpe instanceof Protein) {
				proteinNames.add(((Protein)bpe).getDisplayName());
			} else if (bpe instanceof Pathway) {
				pathwayNames.add(((Pathway)bpe).getDisplayName());
			} else if (bpe instanceof Named) {
				otherNames.add(((Named)bpe).getDisplayName());
			}
			
		}
		
		model.addAttribute("pathwayNames", pathwayNames);
		model.addAttribute("proteinNames",proteinNames);
		model.addAttribute("interactionNames",interactionNames);
		model.addAttribute("otherNames", otherNames);
	}
}