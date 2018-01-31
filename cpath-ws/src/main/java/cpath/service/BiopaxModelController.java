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
    	if(bindingResult.hasErrors()) {
    		errorResponse(args.getCommand(), new ErrorResponse(Status.BAD_REQUEST, errorFromBindingResult(bindingResult)),
					request, response, args.getUser());
    	} else {
			String[] uris = args.getUri();
			Map<String,String> options = new HashMap<String,String>();
			if(args.getPattern()!=null && args.getPattern().length>0) {
				options.put("pattern", StringUtils.join(args.getPattern(), ","));
			}
			ServiceResponse result = service.fetch(args.getFormat(), options, args.getSubpw(), uris);
			stringResponse(args, result, request, response);
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
		ServiceResponse results = service.topPathways(q, organism, datasource);
		
		if(results instanceof ErrorResponse) {
			errorResponse("top_pathways", (ErrorResponse) results, request, response, user);
			return null;
		} else {
			SearchResponse hits = (SearchResponse) results;

			final String ip = clientIpAddress(request);
			final String action = "top_pathways";
			// log/track one data access event for each data provider listed in the result
			service.track(ip, "command", q, action, user);
			for(String provider : hits.getProviders()) {
				service.track(ip,"provider", provider, action, user);
			}

			hits.setVersion(CPathSettings.getInstance().getVersion());
	    	return hits;
		}
    }
    
    
    @RequestMapping("/traverse")
    public TraverseResponse traverse(@Valid Traverse args,
    	BindingResult bindingResult, HttpServletRequest request, HttpServletResponse response)
    {
		final String action = args.getCommand();
    	if(bindingResult.hasErrors()) {
    		errorResponse(action, new ErrorResponse(Status.BAD_REQUEST, errorFromBindingResult(bindingResult)),
					request, response, args.getUser());
    	} else {
    		ServiceResponse sr = service.traverse(args.getPath(), args.getUri());
    		if(sr instanceof ErrorResponse) {
				errorResponse(action, (ErrorResponse) sr, request, response, args.getUser());
			} else {
				final String ip = clientIpAddress(request);
				service.track(ip, "command", args.getLabel(), action, args.getUser());
				//TODO: log/track data providers that occur is the traverse query result
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
		//check for binding errors
		if(bindingResult.hasErrors()) {
			errorResponse(args.getCommand(), new ErrorResponse(Status.BAD_REQUEST, errorFromBindingResult(bindingResult)),
					request, response, args.getUser());
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
			String msg = getClass().getCanonicalName() + " does not support " + args.getKind();
			errorResponse(args.getCommand(), new ErrorResponse(Status.INTERNAL_ERROR, msg),
					request, response, args.getUser());
			return;
		}

		// write the result and log/track the service access events
		stringResponse(args, result, request, response);
    }
	
    @RequestMapping(value="/search")
    public SearchResponse search(@Valid Search args, BindingResult bindingResult,
								 HttpServletRequest request, HttpServletResponse response)
    {
    	if(bindingResult.hasErrors()) {
			errorResponse(args.getCommand(), new ErrorResponse(Status.BAD_REQUEST, errorFromBindingResult(bindingResult)),
					request, response, args.getUser());
			return null;
		} else {
			// get results from the service
			ServiceResponse results = service.search(args.getQ(), args.getPage(), args.getType(),
					args.getDatasource(), args.getOrganism());

			if(results instanceof ErrorResponse) {
				errorResponse(args.getCommand(), (ErrorResponse) results, request, response, args.getUser());
				return null;
			} else {
				final String ip = clientIpAddress(request);
				final String action = args.getCommand();
				final String client = args.getUser();
				// log/track one data access event for each data provider listed in the result
				service.track(ip, "command", args.getLabel(), action, client);
				for(String provider : ((SearchResponse)results).getProviders()) {
					service.track(ip,"provider", provider, action, client);
				}

				SearchResponse searchResponse = (SearchResponse) results;
				searchResponse.setVersion(CPathSettings.getInstance().getVersion());
				return searchResponse;
			}
		}
	}
    
}