package cpath.service;

import java.util.*;

import cpath.service.args.*;
import cpath.service.jaxb.*;
import cpath.service.args.binding.*;

import org.apache.commons.lang3.StringUtils;
import org.biopax.paxtools.model.level3.Protein;
import org.biopax.paxtools.pattern.miner.SIFType;
import org.biopax.paxtools.query.algorithm.Direction;
import org.biopax.paxtools.query.algorithm.LimitType;

import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

/**
 * cPathSquared Model Access Web Service.
 */
@RestController
@RequestMapping(method = RequestMethod.GET)
public class BiopaxModelController extends BasicController {

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
			errorResponse(args, new ErrorResponse(Status.BAD_REQUEST, errorFromBindingResult(bindingResult)),
					request, response);
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
	public SearchResponse topPathways(@Valid TopPathways args, BindingResult bindingResult,
									  HttpServletRequest request, HttpServletResponse response)
	{

		if(bindingResult.hasErrors()) {
			errorResponse(args, new ErrorResponse(Status.BAD_REQUEST,
					errorFromBindingResult(bindingResult)), request, response);
			return null;
		} else {
			ServiceResponse results = service.topPathways(args.getQ(), args.getOrganism(), args.getDatasource());
			if (results instanceof ErrorResponse) {
				errorResponse(args, (ErrorResponse) results, request, response);
				return null;
			} else {
				SearchResponse hits = (SearchResponse) results;
				// log/track data access events
				track(request, args, hits.getProviders(), null);
				hits.setVersion(service.settings().getProviderVersion());
				return hits;
			}
		}
	}


	@RequestMapping("/traverse")
	public TraverseResponse traverse(@Valid Traverse args, BindingResult bindingResult,
									 HttpServletRequest request, HttpServletResponse response)
	{
		if(bindingResult.hasErrors()) {
			errorResponse(args, new ErrorResponse(Status.BAD_REQUEST, errorFromBindingResult(bindingResult)),
					request, response);
		} else {
			ServiceResponse sr = service.traverse(args.getPath(), args.getUri());
			if(sr instanceof ErrorResponse) {
				errorResponse(args, (ErrorResponse) sr, request, response);
			} else {
				track(request, args, null, null);
				//TODO: log/track data providers that occur is the traverse query result
				TraverseResponse traverseResponse = (TraverseResponse) sr;
				traverseResponse.setVersion(service.settings().getProviderVersion());
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
			errorResponse(args, new ErrorResponse(Status.BAD_REQUEST, errorFromBindingResult(bindingResult)),
					request, response);
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
				errorResponse(args, new ErrorResponse(Status.INTERNAL_ERROR, msg),
						request, response);
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
			errorResponse(args, new ErrorResponse(Status.BAD_REQUEST,
					errorFromBindingResult(bindingResult)), request, response);
			return null;
		} else {
			// get results from the service
			ServiceResponse results = service.search(args.getQ(), args.getPage(), args.getType(),
					args.getDatasource(), args.getOrganism());

			if(results instanceof ErrorResponse) {
				errorResponse(args, (ErrorResponse) results, request, response);
				return null;
			} else {
				// log/track one data access event for each data provider listed in the result
				track(request, args, ((SearchResponse)results).getProviders(), null);
				SearchResponse searchResponse = (SearchResponse) results;
				searchResponse.setVersion(service.settings().getProviderVersion());
				return searchResponse;
			}
		}
	}

}