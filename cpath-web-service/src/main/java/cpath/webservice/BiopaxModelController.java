package cpath.webservice;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

import cpath.config.CPathSettings;
import cpath.service.LogEvent;
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
	 * This is to make cPath2 data more LinkedData compatible by making all the BioPAX object URIs resolvable.
	 * 
	 * Normally, one should use #elementById(Get, BindingResult, Writer, HttpServletRequest, HttpServletResponse)
	 * query instead of posting a PC2 BioPAX URI like 'http://purl.org/pc2/8/psp' in a browser's address line directly.
	 * Nevertheless, all such URIs must be resolved, and this method does it, if possible.
	 * 
	 * @param localId - the part of URI following xml:base
	 * 
	 * TODO return a summary page (type,name, some stats, links to biopax/sif/gsea data) instead of raw BioPAX.
	 */
	@RequestMapping(method=RequestMethod.GET, value="/{localId}")
	public void cpathIdInfo(@PathVariable String localId, Writer writer, 
			HttpServletRequest request, HttpServletResponse response) throws Exception
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
			localId = localId.replaceAll(":", "%3A").replaceAll("#", "%23").replaceAll(" ", "+");
		String maybeUri = xmlBase + localId;

		if(service.getModel().containsID(maybeUri)) {
			Get get = new Get();
			get.setUri(new String[]{maybeUri});
			// delegate this task to "/get" (by URI/ID) command
			elementById(get, null, writer, request, response);
		} else {
			//no other access log events are recorded in this case
			//(i.e,, when neither URI/object nor page/controller exist)
			errorResponse(Status.NO_RESULTS_FOUND, "", response);
		}
	}
	
	
	// Get by BioPAX URI (or bio ID) command
    @RequestMapping("/get")
    public void elementById(@Valid Get get, BindingResult bindingResult, 
    	Writer writer, HttpServletRequest request, HttpServletResponse response) throws IOException
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
    	Set<LogEvent> events = new HashSet<LogEvent>();
    	events.add(LogEvent.from(Cmd.TOP_PATHWAYS));
		
		ServiceResponse results = service.topPathways(organism, datasource);
		
		if(results instanceof ErrorResponse) {
			errorResponse(((ErrorResponse) results).getStatus(), 
					((ErrorResponse) results).toString(), request, response, events);
		} else if(results.isEmpty()) {
			errorResponse(Status.NO_RESULTS_FOUND, 
					"no hits", request, response, events);
		} else {//return results
			//log to db
			SearchResponse hits = (SearchResponse) results;
    		events.addAll(LogEvent.fromProviders(hits.getProviders()));
	    	service.log(events, clientIpAddress(request));
			return hits;
		}
		
		return null;
    }
    
    
    @RequestMapping("/traverse")
    public @ResponseBody ServiceResponse traverse(@Valid Traverse query, 
    	BindingResult bindingResult, HttpServletRequest request, HttpServletResponse response) 
    		throws IOException 
    {
    	Set<LogEvent> events = new HashSet<LogEvent>();
    	events.add(LogEvent.from(Cmd.TRAVERSE));
    	
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
				errorResponse(((ErrorResponse) results).getStatus(), 
					((ErrorResponse) results).toString(), 
						request, response, events);
			} else if(results.isEmpty()) {
				errorResponse(Status.NO_RESULTS_FOUND, 
						"no hits", request, response, events);
			} else {
				//count for all unique provider names from the ServiceResponse
	    		events.addAll(LogEvent.fromProviders(
	    				((SearchResponse)results).getProviders()
	    			));
				//save to the log db
		    	service.log(events, clientIpAddress(request));
				return results;
			}
			return null;
		}
	}	
    
}