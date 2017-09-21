package cpath.service;

import java.io.IOException;
import java.util.*;

import cpath.config.CPathSettings;
import cpath.service.jaxb.*;
import cpath.service.args.Get;
import cpath.service.args.Graph;
import cpath.service.args.Search;
import cpath.service.args.Traverse;
import cpath.service.args.binding.*;

import org.apache.commons.lang3.StringUtils;
import org.biopax.paxtools.model.level3.Protein;
import org.biopax.paxtools.pattern.miner.SIFType;
import org.biopax.paxtools.query.algorithm.Direction;
import org.biopax.paxtools.query.algorithm.LimitType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
@RestController
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
		binder.registerCustomEditor(SIFType.class, new SIFTypeEditor());
        binder.registerCustomEditor(Class.class, new BiopaxTypeEditor());
    }


	/**
	 * A very simple description of a BioPAX object;
	 * works only for those BioPAX objects that have URIs
	 * based on xml:base (namespace) which is the URL (or proxy)
	 * for this web service endpoint.
	 *
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
		/* A hack
		 * (works for some clients/browsers;
		 * a better solution would be to never generate biopax URIs
		 * that contain url-encoded sharps, colons, spaces, etc.)
		 *
		 * The localId value gets here url-un-encoded;
		 * so, we have to url-encode ":","#"," " back - replace with
		 * '%3A', '%23', "+" respectively - to recover the original PC URI
		 * (were we simply concatenate xml:base + localId, the
		 * result would be not the original URI in some cases).
		 */
		if(localId.startsWith("#"))
			localId = localId.substring(1);

		if(localId.contains(":") || localId.contains("#") || localId.contains(" "))
			localId = localId.replaceAll(":", "%3A").replaceAll("#", "%23").replaceAll(" ", "+");

		String maybeUri = xmlBase + localId;
		log.debug("trying /get?uri=" + maybeUri);

		ServiceResponse result = service.fetch(OutputFormat.JSONLD, null, false, maybeUri);
		Set<LogEvent> events = new HashSet<LogEvent>();
		events.add(LogEvent.format(OutputFormat.JSONLD));
		events.add(LogEvent.command(Cmd.GET));
		stringResponse(result, request, response, events);
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
			String[] uris = args.getUri();

			Map<String,String> options = new HashMap<String,String>();
			if(args.getPattern()!=null && args.getPattern().length>0)
				options.put("pattern", StringUtils.join(args.getPattern(),","));

			ServiceResponse result = service.fetch(format, options, args.getSubpw(), uris);
			events.add(LogEvent.format(format));
			stringResponse(result, request, response, events);
		}
    }  


	@RequestMapping("/top_pathways")
    public SearchResponse topPathways(
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
			return null;
		} else {
			//log
			SearchResponse hits = (SearchResponse) results;
			hits.setVersion(CPathSettings.getInstance().getVersion());
    		events.addAll(LogEvent.providers(hits.getProviders()));
	    	service.log(events, clientIpAddress(request));
			return hits;
		}
    }
    
    
    @RequestMapping("/traverse")
    public TraverseResponse traverse(@Valid Traverse args,
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
				errorResponse(((ErrorResponse) sr).getStatus(), sr.toString(), request, response, events);
			} else {
    			//log to db and return the xml object
    			service.log(events, clientIpAddress(request));
				//TODO: (how) log provider names with each traverse query result?..
				TraverseResponse traverseResponse = (TraverseResponse) sr;
				traverseResponse.setVersion(CPathSettings.getInstance().getVersion());
    			return traverseResponse;
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

		Map<String,String> formatOptions = new HashMap<String,String>();
		if(args.getPattern()!=null && args.getPattern().length>0)
			formatOptions.put("pattern", StringUtils.join(args.getPattern(),","));
		
		switch (args.getKind()) {
		case NEIGHBORHOOD:
			result = service.getNeighborhood(args.getFormat(), formatOptions, args.getSource(),
				args.getLimit(), args.getDirection(), args.getOrganism(), args.getDatasource(), args.getSubpw());
			break;
		case PATHSBETWEEN:
			result = service.getPathsBetween(args.getFormat(), formatOptions, args.getSource(),
				args.getLimit(), args.getOrganism(), args.getDatasource(), args.getSubpw());
			break;
		case PATHSFROMTO:
			result = service.getPathsFromTo(args.getFormat(), formatOptions, args.getSource(),
				args.getTarget(), args.getLimit(), args.getOrganism(), args.getDatasource(), args.getSubpw());
			break;
		case COMMONSTREAM:
			result = service.getCommonStream(args.getFormat(), formatOptions, args.getSource(),
				args.getLimit(), args.getDirection(), args.getOrganism(), args.getDatasource(), args.getSubpw());
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
    public SearchResponse search(@Valid Search args, BindingResult bindingResult,
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
			ServiceResponse results = service.search(args.getQ(), args.getPage(), args.getType(),
					args.getDatasource(), args.getOrganism());

			if(results instanceof ErrorResponse) {
				errorResponse(((ErrorResponse) results).getStatus(), results.toString(), request, response, events);
				return null;
			} else {
				if(!results.isEmpty()) {
					//count for all unique provider names from the ServiceResponse
					events.addAll(LogEvent.providers(
							((SearchResponse) results).getProviders()
					));
				}
				//save to the log db
		    	service.log(events, clientIpAddress(request));
				SearchResponse searchResponse = (SearchResponse) results;
				searchResponse.setVersion(CPathSettings.getInstance().getVersion());

				return searchResponse;
			}
		}
	}
    
}