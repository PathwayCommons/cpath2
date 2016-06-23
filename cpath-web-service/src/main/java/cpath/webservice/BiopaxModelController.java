package cpath.webservice;

import java.io.IOException;
import java.util.*;

import cpath.config.CPathSettings;
import cpath.service.*;
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

import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Protein;
import org.biopax.paxtools.query.algorithm.Direction;
import org.biopax.paxtools.query.algorithm.LimitType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final Logger log = LoggerFactory.getLogger(BiopaxModelController.class);
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
	 * A very simple description of a BioPAX object identified by the cPath2-generated URI.
	 * TODO: make a human-readable rich description page with links and images...
	 *
	 * @param localId - the part of URI following xml:base
	 * @param request web request
	 * @param response web response
	 */
	@RequestMapping(method= RequestMethod.GET, value="/{localId}")
	public void cpathIdInfo(@PathVariable String localId, HttpServletRequest request, HttpServletResponse response)
			throws IOException
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
		log.debug("trying /get?uri=" + maybeUri);

		Model model = service.getModel();
		if(service.getModel() != null)
		{
			BioPAXElement bpe = model.getByID(maybeUri);
			if (bpe != null) {
				//convert a single object (incomplete) to JSON-LD (unlike '/get', which extracts a sub-model)
//		return String.format("%s %s %s", bpe.getUri(), bpe.getModelInterface().getSimpleName(), bpe.toString());
				Model m = BioPAXLevel.L3.getDefaultFactory().createModel();
				m.setXmlBase(xmlBase);
				m.add(bpe);
				//TODO auto-complete (does it makes sense)?
				ServiceResponse sr = new BiopaxConverter(null).convert(m, OutputFormat.JSONLD);
				Set<LogEvent> events = new HashSet<LogEvent>();
				events.add(LogEvent.format(OutputFormat.JSONLD));
				stringResponse(sr, request, response, events); //also deletes the tmp data file
			} else {
				response.sendError(404, "No BioPAX element found; URI: " + maybeUri); //no resource available
			}
		}
		else { //looks like - debug mode
			response.sendError(503, "Please try again later"); //unavailable (starting.. or maintenance mode)
		}
	}

	// Get by ID (URI) command
    @RequestMapping("/get")
    public void elementById(@Valid Get args, BindingResult bindingResult,
							HttpServletRequest request, HttpServletResponse response)
    {
		//log events: command, format
    	Set<LogEvent> events = new HashSet<LogEvent>();
    	events.add(LogEvent.command(Cmd.GET));
		if(args.getUser()!=null && !args.getUser().isEmpty())
			events.add(LogEvent.client(args.getUser()));
    	
    	if(bindingResult.hasErrors()) {
    		errorResponse(Status.BAD_REQUEST, 
    				errorFromBindingResult(bindingResult), request, response, events);
    	} else {
			OutputFormat format = args.getFormat();
			String[] uri = args.getUri();
			ServiceResponse result = service.fetch(format, uri);
			events.add(LogEvent.format(format));
			stringResponse(result, request, response, events);
		}
    }  


	@RequestMapping("/top_pathways")
    public @ResponseBody SearchResponse topPathways(
			@RequestParam(required=false) String q,
    		@RequestParam(required=false) String[] datasource,
			@RequestParam(required=false) String[] organism,
			@RequestParam(required=false) String user,
			HttpServletRequest request, HttpServletResponse response)
    {
    	Set<LogEvent> events = new HashSet<LogEvent>();
    	events.add(LogEvent.command(Cmd.TOP_PATHWAYS));
		if(user!=null && !user.isEmpty())
			events.add(LogEvent.client(user));

		ServiceResponse results = service.topPathways(q, organism, datasource);
		
		if(results instanceof ErrorResponse) {
			errorResponse(((ErrorResponse) results).getStatus(), 
					((ErrorResponse) results).toString(), request, response, events);
		} else if(results.isEmpty()) {
			errorResponse(Status.NO_RESULTS_FOUND, "no hits", request, response, events);
		} else {//return results
			//log to db
			SearchResponse hits = (SearchResponse) results;
    		events.addAll(LogEvent.providers(hits.getProviders()));
	    	service.log(events, clientIpAddress(request));
			return hits;
		}
		
		return null;
    }
    
    
    @RequestMapping("/traverse")
    public @ResponseBody TraverseResponse traverse(@Valid Traverse args,
    	BindingResult bindingResult, HttpServletRequest request, HttpServletResponse response)
    {
    	Set<LogEvent> events = new HashSet<LogEvent>();
    	events.add(LogEvent.command(Cmd.TRAVERSE));
		if(args.getUser()!=null && !args.getUser().isEmpty())
			events.add(LogEvent.client(args.getUser()));
    	
    	if(bindingResult.hasErrors()) {
    		errorResponse(Status.BAD_REQUEST, 
    				errorFromBindingResult(bindingResult), request, response, events);
    	} else {
    		ServiceResponse sr = service.traverse(args.getPath(), args.getUri());
    		if(sr instanceof ErrorResponse) {
				errorResponse(((ErrorResponse) sr).getStatus(), 
						((ErrorResponse) sr).toString(), request, response, events);
			}
    		else if(sr == null || sr.isEmpty()) {
    			errorResponse(Status.NO_RESULTS_FOUND, "no results found", request, response, events);
    		}
    		else {
    			//log to db and return the xml object
    			service.log(events, clientIpAddress(request));
    			return (TraverseResponse) sr;
			}
    	}
    	return null;
    }
 
    
	@RequestMapping("/graph")
	public void graphQuery(@Valid Graph args, BindingResult bindingResult,
						   HttpServletRequest request, HttpServletResponse response)
    {

    	Set<LogEvent> events = new HashSet<LogEvent>();
    	events.add(LogEvent.command(Cmd.GRAPH));
		if(args.getUser()!=null && !args.getUser().isEmpty())
			events.add(LogEvent.client(args.getUser()));
		
		//check for binding errors
		if(bindingResult.hasErrors()) {
			errorResponse(Status.BAD_REQUEST, 
					errorFromBindingResult(bindingResult), request, response, events);
			return;
		} 
		
    	// on parameter binding success, add a few more events to log/count -
		events.add(LogEvent.kind(args.getKind()));
    	events.add(LogEvent.format(args.getFormat()));
		
		ServiceResponse result;
		
		switch (args.getKind()) {
		case NEIGHBORHOOD:
			result = service.getNeighborhood(args.getFormat(), args.getSource(),
				args.getLimit(), args.getDirection(), args.getOrganism(), args.getDatasource());
			break;
		case PATHSBETWEEN:
			result = service.getPathsBetween(args.getFormat(), args.getSource(),
				args.getLimit(), args.getOrganism(), args.getDatasource());
			break;
		case PATHSFROMTO:
			result = service.getPathsFromTo(args.getFormat(), args.getSource(),
				args.getTarget(), args.getLimit(), args.getOrganism(), args.getDatasource());
			break;
		case COMMONSTREAM:
			result = service.getCommonStream(args.getFormat(), args.getSource(),
				args.getLimit(), args.getDirection(), args.getOrganism(), args.getDatasource());
			break;
		default:
			// impossible (should have failed earlier)
			errorResponse(Status.INTERNAL_ERROR, 
				getClass().getCanonicalName() + " does not support " 
					+ args.getKind(), request, response, events);
			return;
		}
		
		stringResponse(result, request, response, events);
    }
	
	
    @RequestMapping(value="/search")
    public @ResponseBody SearchResponse search(@Valid Search args, BindingResult bindingResult,
											   HttpServletRequest request, HttpServletResponse response)
    {		
		// Prepare service assess events
		// (won't use 'datasource' filter values for the access log, PROVIDER type events)
    	Set<LogEvent> events = new HashSet<LogEvent>();
    	events.add(LogEvent.command(Cmd.SEARCH));
		if(args.getUser()!=null && !args.getUser().isEmpty())
			events.add(LogEvent.client(args.getUser()));
    	   	
    	if(bindingResult.hasErrors()) {
			errorResponse(Status.BAD_REQUEST, 
				errorFromBindingResult(bindingResult), 
					request, response, events);
			return null;
		} else {
			// get results from the service
			ServiceResponse results = service.search(
					args.getQ(), args.getPage(), args.getType(),
					args.getDatasource(), args.getOrganism());

			if(results instanceof ErrorResponse) {
				errorResponse(((ErrorResponse) results).getStatus(), results.toString(), request, response, events);
			} else if(results.isEmpty()) {
				errorResponse(Status.NO_RESULTS_FOUND, "no hits", request, response, events);
			} else {
				//count for all unique provider names from the ServiceResponse
	    		events.addAll(LogEvent.providers(
	    				((SearchResponse)results).getProviders()
	    			));
				//save to the log db
		    	service.log(events, clientIpAddress(request));
				return (SearchResponse) results;
			}
			return null;
		}
	}
    
}