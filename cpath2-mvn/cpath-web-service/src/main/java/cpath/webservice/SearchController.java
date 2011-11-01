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

import java.util.Set;
import java.util.HashSet;

import cpath.dao.filters.SearchFilter;
import cpath.dao.internal.filters.EntityByOrganismRelationshipXrefsFilter;
import cpath.dao.internal.filters.SequenceEntityReferenceOrganismFilter;
import cpath.dao.internal.filters.EntityDataSourceFilter;
import cpath.service.CPathService;
import cpath.service.jaxb.*;
import cpath.webservice.args.*;
import cpath.webservice.args.binding.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.model.level3.Entity;
import org.biopax.paxtools.model.level3.Protein;
import org.biopax.paxtools.model.level3.SequenceEntityReference;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.Valid;

/**
 * cPathSquared Search Web Service.
 * 
 * @author rodche
 */
@Controller
public class SearchController extends BasicController {
    private static final Log log = LogFactory.getLog(SearchController.class);    
	
    private CPathService service; // main PC db access
	
    public SearchController(CPathService service) {
		this.service = service;
	}
    
    /**
	 * This configures the web request parameters binding, i.e., 
	 * conversion to the corresponding java types; for example,
	 * "protein" is recognized as {@link Protein}, etc.
	 *  Illegal query parameters result in binding errors.
	 * 
	 * @param binder
	 */
	@InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Class.class, new BiopaxTypeEditor());
        binder.registerCustomEditor(OrganismDataSource.class, new OrganismDataSourceEditor());
        binder.registerCustomEditor(PathwayDataSource.class, new PathwayDataSourceEditor());
    }
	
    
    private Set<SearchFilter> createFilters(OrganismDataSource[] organisms,
			PathwayDataSource[] dataSources )
	{
		Set<SearchFilter> searchFilters = new HashSet<SearchFilter>();
		
		if(organisms != null && organisms.length>0) { // it's optional parameter (can be null)
			String[] organismURIs = new String[organisms.length];
			int i = 0;
			for(OrganismDataSource o : organisms) {
				organismURIs[i++] = o.getURI(); // cannot be null (anymore, due to the changes in the arg editor/binder)
			}
			// filter for entities (uses auto-generated by the normalizer special rel. xrefs)
			SearchFilter<Entity, String> byOrganismRXFilter = new EntityByOrganismRelationshipXrefsFilter();
			byOrganismRXFilter.setValues(organismURIs);
			searchFilters.add(byOrganismRXFilter);
			// filter for qualified utility classes (uses BioPAX property 'organism' directly)
			SearchFilter<SequenceEntityReference, String> byOrganismFilter = new SequenceEntityReferenceOrganismFilter();
			byOrganismFilter.setValues(organismURIs);
			searchFilters.add(byOrganismFilter);
		}
		
		if(dataSources != null && dataSources.length > 0) { // because of being optional arg.
			String[] dsourceURIs = new String[dataSources.length];
			int i = 0;
			for(PathwayDataSource d : dataSources) {
				dsourceURIs[i++] = d.asDataSource().getSystemCode();
			}
			SearchFilter<Entity, String> byDatasourceFilter = new EntityDataSourceFilter();
			byDatasourceFilter.setValues(dsourceURIs);
			searchFilters.add(byDatasourceFilter);
		}
		
		return searchFilters;
	}


    @RequestMapping(value="/find")
    public @ResponseBody ServiceResponse find(@Valid Search search, BindingResult bindingResult)
    {		
		if(bindingResult.hasErrors()) {
			return errorfromBindingResult(bindingResult);
		} else {
			if (log.isDebugEnabled())
				log.debug("/find called (for type: " 
					+ ((search.getType() == null)? "ALL" : search.getType()) 
					+ "), query:" + search.getQ() + ", page #" + search.getPage());

			Set<SearchFilter> searchFilters = createFilters(
					search.getOrganism(), search.getDatasource());

			// get results from the service
			ServiceResponse results = service.findElements(
					search.getQ(), search.getPage(), search.getType(),
					searchFilters.toArray(new SearchFilter[] {}));

			return results;
		}
	}

    
	@RequestMapping(value="/find_entity")
    public @ResponseBody ServiceResponse findEntities(@Valid Search search, BindingResult bindingResult)
    {		
		if(bindingResult.hasErrors()) {
			return errorfromBindingResult(bindingResult);
		} else {
			if (log.isDebugEnabled())
				log.debug("/find_entity called (for type: " 
						+ ((search.getType() == null)? "ALL" : search.getType()) 
						+ "), query:" + search.getQ() + ", page #" + search.getPage());

			Set<SearchFilter> searchFilters = createFilters(
					search.getOrganism(), search.getDatasource());

			// get results from the service
			ServiceResponse results = service.findEntities(
					search.getQ(), search.getPage(), search.getType(),
					searchFilters.toArray(new SearchFilter[] {}));

			return results;
		}
	}
	
}