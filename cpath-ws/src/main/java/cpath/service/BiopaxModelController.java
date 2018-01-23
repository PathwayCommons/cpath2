package cpath.service;

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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
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

	// Get by ID (URI) command
    @RequestMapping("/get")
    public void elementById(@Valid Get args, BindingResult bindingResult,
							HttpServletRequest request, HttpServletResponse response)
    {
		JSONObject event = new JSONObject();
		event.put("command","get");
		OutputFormat format = args.getFormat();
		event.put("format", format.toString().toLowerCase());
		if(args.getUser()!=null && !args.getUser().isEmpty())
			event.put("client", args.getUser());

    	if(bindingResult.hasErrors()) {
    		errorResponse(Status.BAD_REQUEST, 
    				errorFromBindingResult(bindingResult), request, response, event);
    	} else {
			String[] uris = args.getUri();
			Map<String,String> options = new HashMap<String,String>();
			if(args.getPattern()!=null && args.getPattern().length>0)
				options.put("pattern", StringUtils.join(args.getPattern(),","));

			ServiceResponse result = service.fetch(format, options, args.getSubpw(), uris);

			stringResponse(result, request, response, event);
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
		JSONObject event = new JSONObject();
		event.put("command","top_pathways");
		event.put("format","search"); //the same xml or json output format as for /search
		if(user!=null && user.isEmpty())
			event.put("client", user);

		ServiceResponse results = service.topPathways(q, organism, datasource);
		
		if(results instanceof ErrorResponse) {
			errorResponse(((ErrorResponse) results).getStatus(), 
					((ErrorResponse) results).toString(), request, response, event);
			return null;
		} else {
			SearchResponse hits = (SearchResponse) results;
			JSONArray providers = new JSONArray();
			providers.addAll(hits.getProviders());
			event.put("provider", providers);

	    	service.track(event); //log, track

			hits.setVersion(CPathSettings.getInstance().getVersion());
	    	return hits;
		}
    }
    
    
    @RequestMapping("/traverse")
    public TraverseResponse traverse(@Valid Traverse args,
    	BindingResult bindingResult, HttpServletRequest request, HttpServletResponse response)
    {
		JSONObject event = new JSONObject();
		event.put("command","traverse");
		event.put("format","traverse"); //traverse schema - xml or json
		if(args.getUser()!=null && !args.getUser().isEmpty())
			event.put("client", args.getUser());

    	if(bindingResult.hasErrors()) {
    		errorResponse(Status.BAD_REQUEST, errorFromBindingResult(bindingResult), request, response, event);
    	} else {
    		ServiceResponse sr = service.traverse(args.getPath(), args.getUri());
    		if(sr instanceof ErrorResponse) {
				errorResponse(((ErrorResponse) sr).getStatus(), sr.toString(), request, response, event);
			} else {
    			service.track(event);
				//TODO: (how) log providers with each traverse query result?..
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
		JSONObject event = new JSONObject();
		event.put("command", "graph/" + args.getKind().toString().toLowerCase());
		event.put("format", args.getFormat().toString().toLowerCase());
		if(args.getUser()!=null && !args.getUser().isEmpty())
			event.put("client", args.getUser());

		//check for binding errors
		if(bindingResult.hasErrors()) {
			errorResponse(Status.BAD_REQUEST, 
					errorFromBindingResult(bindingResult), request, response, event);
			return;
		} 

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
					+ args.getKind(), request, response, event);
			return;
		}
		
		stringResponse(result, request, response, event);
    }
	
	
    @RequestMapping(value="/search")
    public SearchResponse search(@Valid Search args, BindingResult bindingResult,
								 HttpServletRequest request, HttpServletResponse response)
    {		
		// Track service usage (using result data instead of 'datasource' filter values)
		JSONObject event = new JSONObject();
    	event.put("command","search");
		event.put("format","search");
		if(args.getUser()!=null && !args.getUser().isEmpty())
			event.put("client", args.getUser());
    	   	
    	if(bindingResult.hasErrors()) {
			errorResponse(Status.BAD_REQUEST, errorFromBindingResult(bindingResult), request, response, event);
			return null;
		} else {
			// get results from the service
			ServiceResponse results = service.search(args.getQ(), args.getPage(), args.getType(),
					args.getDatasource(), args.getOrganism());

			if(results instanceof ErrorResponse) {
				errorResponse(((ErrorResponse) results).getStatus(), results.toString(), request, response, event);
				return null;
			} else {
				JSONArray providers = new JSONArray();
				providers.addAll(((SearchResponse)results).getProviders());
				event.put("provider", providers);

		    	service.track(event); //track service usage and data sources in the results

				SearchResponse searchResponse = (SearchResponse) results;
				searchResponse.setVersion(CPathSettings.getInstance().getVersion());
				return searchResponse;
			}
		}
	}
    
}