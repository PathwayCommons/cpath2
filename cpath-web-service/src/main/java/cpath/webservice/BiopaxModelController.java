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
import java.util.*;

import cpath.config.CPathSettings;
import cpath.jpa.LogEvent;
import cpath.service.Cmd;
import cpath.service.ErrorResponse;
import cpath.service.GraphType;
import cpath.service.OutputFormat;
import cpath.service.Status;
import cpath.service.jaxb.*;
import cpath.webservice.args.Get;
import cpath.webservice.args.Graph;
import cpath.webservice.args.Search;
import cpath.webservice.args.Traverse;
import cpath.webservice.args.binding.BiopaxTypeEditor;
import cpath.webservice.args.binding.GraphQueryDirectionEditor;
import cpath.webservice.args.binding.GraphQueryLimitEditor;
import cpath.webservice.args.binding.GraphTypeEditor;
import cpath.webservice.args.binding.OutputFormatEditor;

import org.biopax.paxtools.model.level3.Protein;
import org.biopax.paxtools.query.algorithm.Direction;
import org.biopax.paxtools.query.algorithm.LimitType;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

/**
 * cPathSquared Model Access Web Service.
 * 
 * @author rodche
 */
@Controller
public class BiopaxModelController extends BasicController {
   	
    private static final String xmlBase = CPathSettings.getInstance().getXmlBase();
	
    
    /**
	 * This configures the web request parameters binding, i.e., 
	 * conversion to the corresponding java types; for example,
	 * "neighborhood" is recognized as {@link GraphType#NEIGHBORHOOD},  
	 *  "protein" - {@link Protein} , etc.
	 *  Depending on the editor, illegal query parameters may result 
	 *  in an error or just NULL value.
	 * 
	 * @param binder
	 */
	@InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(GraphType.class, new GraphTypeEditor());
        binder.registerCustomEditor(Direction.class, new GraphQueryDirectionEditor());
        binder.registerCustomEditor(LimitType.class, new GraphQueryLimitEditor());
        binder.registerCustomEditor(OutputFormat.class, new OutputFormatEditor());
        binder.registerCustomEditor(Class.class, new BiopaxTypeEditor());
    }
	
	
	/**
	 * This is a convenience method is to make cpath2 data 
	 * more suitable for LinkedData / Semantic Web, by 
	 * resolving cpath2-generated URIs (created in the data 
	 * warehouse and during validation and normalization). 
	 * Ideally, all URIs are to be resolvable URLs.
	 * 
	 * Normally, one should use #elementById(Get, BindingResult, Writer, HttpServletRequest, HttpServletResponse).
	 * 
	 * @param localId - the part of URI following xml:base
	 * 
	 * TODO return a summary (view) instead of plain raw BioPAX content.
	 */
	@RequestMapping(method=RequestMethod.GET, value="/{localId}")
	public void cpathIdInfo(@PathVariable String localId, Writer writer, 
			HttpServletRequest request, HttpServletResponse response) 
					throws Exception 
	{
			/* A hack (specific to our normalizer and also 
			 * might not work for all client links/browsers...
			 * a better solution would be never generate tricky URIs,
			 * containing encoded sharps, colons, spaces, etc.): 
			 * the 'localId' parameter is usually un-encoded by the frameworks;
			 * so we need to encode ":","#"," " back to 
			 * %3A, %23, and "+" respectively, to get the original URI
			 * (otherwise, if we simply combine localId and xml:base, the 
			 * the resulting "URI" will be non-existing or wrong one)
			 */
			if(localId.startsWith("#"))
				localId = localId.substring(1);
				
			if(localId.contains(":") || localId.contains("#") || localId.contains(" "))
				localId = localId
						.replaceAll(":", "%3A")
							.replaceAll("#", "%23")
								.replaceAll(" ", "+");
			Get get = new Get();			
			get.setUri(new String[]{xmlBase + localId});
			elementById(get, null, writer, request, response);			
	}
	
	
	// Get by ID (URI) command
    @RequestMapping("/get")
    public void elementById(@Valid Get get, BindingResult bindingResult, 
    	Writer writer, HttpServletRequest request, HttpServletResponse response) 
    		throws IOException
    {
		//log events: command, format
    	Set<LogEvent> events = new HashSet<LogEvent>();
    	events.add(LogEvent.from(Cmd.GET));
    	
    	if(bindingResult != null &&  bindingResult.hasErrors()) {
    		errorResponse(Status.BAD_REQUEST, 
    				errorFromBindingResult(bindingResult), request, response, events);
    	} else {
			OutputFormat format = get.getFormat();
			String[] uri = get.getUri();
			ServiceResponse result = service.fetch(format, uri);
			events.add(LogEvent.from(format));
			stringResponse(result, writer, request, response, events);
		}
    }  


	@RequestMapping("/top_pathways")
    public @ResponseBody SearchResponse topPathways(
    		@RequestParam(required=false) String[] datasource, @RequestParam(required=false) String[] organism, 
    		HttpServletRequest request, HttpServletResponse response) throws IOException
    {
//		log(request, "organisms="+Arrays.toString(organism), "datasource="+Arrays.toString(datasource));
    	Set<LogEvent> events = new HashSet<LogEvent>();
    	events.add(LogEvent.from(Cmd.TOP_PATHWAYS));
    	events.add(LogEvent.FORMAT_OTHER);
		
		SearchResponse results = service.topPathways(organism, datasource);
		
		if(results == null || results.isEmpty()) {
			errorResponse(Status.NO_RESULTS_FOUND, "no hits", request, response, events);
		} else {
			//log to db
			//TODO ? not sure we have to update provides' counters...
    		events.addAll(LogEvent.fromProviders(results.getProviders()));
	    	service.log(events, clientIpAddress(request));
			
			return results;
		}
		
		return null;
    }
    
    
    @RequestMapping("/traverse")
    public @ResponseBody ServiceResponse traverse(@Valid Traverse query, 
    	BindingResult bindingResult, HttpServletRequest request, HttpServletResponse response) 
    		throws IOException 
    {
//    	log(request, "path="+query.getPath());
    	Set<LogEvent> events = new HashSet<LogEvent>();
    	events.add(LogEvent.from(Cmd.TRAVERSE));
    	events.add(LogEvent.FORMAT_OTHER);
    	
    	if(bindingResult.hasErrors()) {
    		errorResponse(Status.BAD_REQUEST, 
    				errorFromBindingResult(bindingResult), request, response, events);
    	} else {
    		ServiceResponse sr = service.traverse(query.getPath(), query.getUri());
    		if(sr instanceof ErrorResponse) {
				errorResponse(((ErrorResponse) sr).getStatus(), 
						((ErrorResponse) sr).toString(), request, response, events);
			}
    		else if(sr == null || sr.isEmpty()) {
    			errorResponse(Status.NO_RESULTS_FOUND, "no results found", request, response, events);
    		}
    		else {
    			//log to db
    			service.log(events, clientIpAddress(request));
    			
    			return sr;
			}
    	}
    	return null;
    }
 
    
	@RequestMapping("/graph")
	public void graphQuery(@Valid Graph graph, BindingResult bindingResult, 
			Writer writer, HttpServletRequest request, HttpServletResponse response) throws IOException
    {

    	Set<LogEvent> events = new HashSet<LogEvent>();
    	events.add(LogEvent.from(Cmd.GRAPH));
		
		//check for binding errors
		if(bindingResult.hasErrors()) {
			errorResponse(Status.BAD_REQUEST, 
					errorFromBindingResult(bindingResult), request, response, events);
			return;
		} 
		
    	// on parameter binding success, add a few more events to log/count -
		events.add(LogEvent.from(graph.getKind()));
    	events.add(LogEvent.from(graph.getFormat()));
		
		ServiceResponse result;
		
		switch (graph.getKind()) {
		case NEIGHBORHOOD:
			result = service.getNeighborhood(graph.getFormat(), graph.getSource(), 
				graph.getLimit(), graph.getDirection(), graph.getOrganism(), graph.getDatasource());
			break;
		case PATHSBETWEEN:
			result = service.getPathsBetween(graph.getFormat(), graph.getSource(), 
				graph.getLimit(), graph.getOrganism(), graph.getDatasource());
			break;
		case PATHSFROMTO:
			result = service.getPathsFromTo(graph.getFormat(), graph.getSource(), 
				graph.getTarget(), graph.getLimit(), graph.getOrganism(), graph.getDatasource());
			break;
		case COMMONSTREAM:
			result = service.getCommonStream(graph.getFormat(), graph.getSource(), 
				graph.getLimit(), graph.getDirection(), graph.getOrganism(), graph.getDatasource());
			break;
		default:
			// impossible (should have failed earlier)
			errorResponse(Status.INTERNAL_ERROR, 
				getClass().getCanonicalName() + " does not support " 
					+ graph.getKind(), request, response, events);			
			return;
		}
		
		stringResponse(result, writer, request, response, events);
    }
	
	
    @RequestMapping(value="/search")
    public @ResponseBody ServiceResponse search(@Valid Search search, 
    		BindingResult bindingResult, HttpServletRequest request, HttpServletResponse response) 
    			throws IOException
    {		
		//prepare to count following service assess events
    	Set<LogEvent> events = new HashSet<LogEvent>();
    	events.add(LogEvent.from(Cmd.SEARCH));
    	events.add(LogEvent.FORMAT_OTHER); //XML or JSON (not BioPAX, SIF, etc.)
    	//do NOT add yet for the 'datasource' filter values can be anything (hard to analyze)		
    	   	
    	if(bindingResult.hasErrors()) {
			errorResponse(Status.BAD_REQUEST, 
				errorFromBindingResult(bindingResult), 
					request, response, events);
			return null;
		} else {
			// get results from the service
			ServiceResponse results = service.search(
					search.getQ(), search.getPage(), search.getType(),
					search.getDatasource(), search.getOrganism());

			if(results instanceof ErrorResponse) {
				errorResponse(Status.INTERNAL_ERROR, 
					((ErrorResponse) results).toString(), 
						request, response, events);
			} else if(results.isEmpty()) {
				errorResponse(Status.NO_RESULTS_FOUND, 
						"no hits", request, response, events);
			} else {
				//count for all unique provider names from the ServiceResponse
				//TODO ? not sure we have to count these...
	    		events.addAll(LogEvent.fromProviders(
	    				((SearchResponse)results).getProviders()
	    			));
				//log to db
		    	service.log(events, clientIpAddress(request));
				return results;
			}
			return null;
		}
	}	
    
}