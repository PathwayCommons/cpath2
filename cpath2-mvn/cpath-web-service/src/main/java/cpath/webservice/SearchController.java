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
import java.io.Writer;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import cpath.dao.filters.SearchFilter;
import cpath.dao.internal.filters.EntityByOrganismRelationshipXrefsFilter;
import cpath.dao.internal.filters.SequenceEntityReferenceOrganismFilter;
//import cpath.dao.internal.filters.EntityByProcessRelationshipXrefsFilter;
import cpath.dao.internal.filters.EntityDataSourceFilter;
import cpath.service.CPathService;
import cpath.service.Cmd;
import cpath.service.GraphType;
import cpath.service.ProtocolStatusCode;
import cpath.service.CPathService.ResultMapKey;
import cpath.service.OutputFormat;
import cpath.service.jaxb.ErrorType;
import cpath.service.jaxb.SearchResponseType;
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
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * cPathSquared Main Web Service.
 * 
 * @author rodche
 */
@Controller
public class SearchController extends BasicController {
    private static final Log log = LogFactory.getLog(SearchController.class);    
	
	
    public SearchController(CPathService service) {
		this.service = service;
	}
    
    /**
	 * This configures the web request parameters binding, i.e., 
	 * conversion to the corresponding java types; for example,
	 * "neighborhood" is recognized as {@link GraphType#NEIGHBORHOOD}, 
	 * "search" will become {@link Cmd#SEARCH}, 
	 *  "protein" - {@link Protein} , etc.
	 *  Depending on the editor, illegal query parameters may result 
	 *  in an error or just NULL value (see e.g., {@link CmdArgsEditor})
	 * 
	 * @param binder
	 */
	@InitBinder
    public void initBinder(WebDataBinder binder) {
		super.initBinder(binder);
		//TODO set SearchValidator
    }

	
	/*
	 * This is for reporting an error "BAD COMMAND"
	 * for everything except for known cpath2 web service
	 * commands (known commands with parameters are mapped 
	 * to more specific controller methods in this class; see below)
	 * 
	 * @param cmd
	 * @return
	 */
	 @RequestMapping("/{cmd}")
	 public @ResponseBody String illegalCommand(@PathVariable String cmd) 
	 {
		 return ProtocolStatusCode.errorAsXml(ProtocolStatusCode.BAD_COMMAND,
			"Unknown command: " + cmd);
	 }
	
	
	/* ========================================================================
	 *    Most Important Web Service Methods
	 * ======================================================================*/
	
	
	// Get by ID (URI) command
    @RequestMapping("/get")
    public void elementById(@Valid Get get, BindingResult bindingResult, Writer writer) throws IOException
    {
    	if(bindingResult.hasErrors()) {
    		ErrorType error = errorfromBindingResult(bindingResult);
    		String str = ProtocolStatusCode.marshal(error);
    		writer.write(str);
    		return;
    	}
    	    	
    	OutputFormat format = get.getFormat();
    	String[] uri = get.getUri();
    	
    	if (log.isInfoEnabled())
			log.info("Query: /get; format:" + format + ", urn:" + Arrays.toString(uri));
    	
    	Map<ResultMapKey, Object> result = service.fetch(format, uri);
    	Object data = parseResultMap(result, format, Arrays.toString(uri), ResultMapKey.DATA);
    	
    	if(data instanceof ErrorType) {
    		writer.write(ProtocolStatusCode.marshal((ErrorType)data));
		} else {
			if(log.isDebugEnabled())
				log.debug("QUERY RETURNED " 
					+ data.toString().length() + " chars");
			writer.write((String) data);
		}
    }  
 
    
    private Set<SearchFilter> createFilters(OrganismDataSource[] organisms,
			PathwayDataSource[] dataSources )
	{
		Set<SearchFilter> searchFilters = new HashSet<SearchFilter>();
		
		if(organisms != null && organisms.length>0) { // it's optional parameter (can be null)
			String[] organismURIs = new String[organisms.length];
			int i = 0;
			for(OrganismDataSource o : organisms) {
				organismURIs[i++] = (o == null) ? "UNKNOWN" : o.getURI();
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
				dsourceURIs[i++] = (d == null) ? "UNKNOWN" : d.asDataSource().getSystemCode();
			}
			SearchFilter<Entity, String> byDatasourceFilter = new EntityDataSourceFilter();
			byDatasourceFilter.setValues(dsourceURIs);
			searchFilters.add(byDatasourceFilter);
		}
		
		return searchFilters;
	}


    @RequestMapping(value="/find")
    public @ResponseBody SearchResponseType find(@Valid Search search, BindingResult bindingResult)
    {		
		if (log.isDebugEnabled())
			log.debug("/find called (for " + search.getType() + "), query:" 
				+ search.getQ() + ", page #" + search.getPage());
    	
    	if(bindingResult.hasErrors()) {
			SearchResponseType response = new SearchResponseType();
			response.setError(errorfromBindingResult(bindingResult));
			return response; // return ERROR
		}
		
		Set<SearchFilter> searchFilters = createFilters(
				search.getOrganism(), search.getDatasource());

		// get results from the service
		Map<ResultMapKey, Object> results = service.findElements(
				search.getQ(), search.getPage(), 
				search.getType(), searchFilters.toArray(new SearchFilter[]{}));
		
		String details = search.getQ() + " (in " + search.getType() + ")";

		// extract data from the message map
		return parseSearchResults(results, details);	
	}

    
	@RequestMapping(value="/entity/find")
    public @ResponseBody SearchResponseType findEntities(@Valid Search search, BindingResult bindingResult)
    {		
		if(bindingResult.hasErrors()) {
			SearchResponseType response = new SearchResponseType();
			response.setError(errorfromBindingResult(bindingResult));
			return response; // return ERROR
		}
		
		if (log.isDebugEnabled())
			log.debug("/entity/find called (for " + search.getType() + "), query:" 
				+ search.getQ() + ", page #" + search.getPage());
		
		Set<SearchFilter> searchFilters = createFilters(
				search.getOrganism(), search.getDatasource());

		// get results from the service
		Map<ResultMapKey, Object> results = service.findEntities(
				search.getQ(), search.getPage(), 
				search.getType(), searchFilters.toArray(new SearchFilter[]{}));
		
		String details = search.getQ() + " (in " + search.getType() + ")";
		// extract data from the message map

		return parseSearchResults(results, details);
	}

	private SearchResponseType parseSearchResults(Map<ResultMapKey, Object> results,
			String details) 
	{
		Object data = parseResultMap(results, null, details, ResultMapKey.DATA);
		if(data instanceof ErrorType) {
			SearchResponseType srt = new SearchResponseType();
			srt.setError((ErrorType)data);
			return srt;
		} else {
			return (SearchResponseType) data;
		}	
	}
}