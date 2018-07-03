package cpath.service;

import java.util.*;

import cpath.config.CPathSettings;
import cpath.service.args.*;
import cpath.service.jaxb.*;
import cpath.service.args.binding.*;

import io.swagger.annotations.ApiOperation;
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
 */
@RestController
@RequestMapping(method = {RequestMethod.GET, RequestMethod.POST})
public class BiopaxModelController extends BasicController {

  private static final Logger log = LoggerFactory.getLogger(BiopaxModelController.class);
  private static final String xmlBase = CPathSettings.getInstance().getXmlBase();

  /**
   * This configures the web request parameters binding, i.e.,
   * conversion to the corresponding java types; for example,
   * "neighborhood" is recognized as {@link GraphType#NEIGHBORHOOD},
   * "protein" - {@link Protein} , etc.
   * Depending on the editor, illegal query parameters may result
   * in an error or just NULL value.
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
  @ApiOperation(value = "Get BioPAX elements (as sub-model) by URIs.",
    notes = "Retrieves pathways, interactions, physical entities from the db by URIs.")
  @RequestMapping("/get")
  public void elementById(@Valid Get args, BindingResult bindingResult,
                          HttpServletRequest request, HttpServletResponse response)
  {
    if (bindingResult.hasErrors()) {
      errorResponse(args, new ErrorResponse(Status.BAD_REQUEST, errorFromBindingResult(bindingResult)),
        request, response);
    } else {
      String[] uris = args.getUri();
      Map<String, String> options = new HashMap<String, String>();
      if (args.getPattern() != null && args.getPattern().length > 0) {
        options.put("pattern", StringUtils.join(args.getPattern(), ","));
      }
      ServiceResponse result = service.fetch(args.getFormat(), options, args.isSubpw(), uris);
      stringResponse(args, result, request, response);
    }
  }

  @ApiOperation(value = "Search for top pathways.",
    notes = "It finds root/parent Pathway objects, i.e, ones that are neither 'controlled' nor a 'pathwayComponent' " +
      "of another biological process; trivial pathways are excluded from the results.")
  @RequestMapping("/top_pathways")
  public SearchResponse topPathways(@Valid TopPathways args, BindingResult bindingResult,
                                    HttpServletRequest request, HttpServletResponse response)
  {

    if (bindingResult.hasErrors()) {
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
        hits.setVersion(CPathSettings.getInstance().getVersion());
        return hits;
      }
    }
  }

  @ApiOperation(value = "Access properties of BioPAX elements using graph path expressions",
  notes = "To collect specific BioPAX property values, use the following " +
    "path accessor format: InitialClass/property[:filterClass]/[property][:filterClass]... " +
    "A \"*\" sign after the property instructs the path accessor to transitively traverse that property. " +
    "For example, the following path accessor will traverse through all physical entity components a complex, " +
    "including components of nested complexes, if any: Complex/component*/entityReference/xref:UnificationXref. " +
    "The next would list display names of all participants of interactions, " +
    "which are pathway components of a pathway: Pathway/pathwayComponent:Interaction/participant*/displayName. " +
    "Optional restriction ':filterClass' enables limiting the property values to a certain sub-class of the " +
    "object property range. In the first example above, this is used to get only the unification xrefs. " +
    "All the official BioPAX properties as well as additional derived classes and properties, such as inverse " +
    "properties and interfaces that represent anonymous union classes in BioPAX OWL can be used in a path accessor.")
  @RequestMapping("/traverse")
  public TraverseResponse traverse(@Valid Traverse args, BindingResult bindingResult,
                                   HttpServletRequest request, HttpServletResponse response)
  {
    if (bindingResult.hasErrors()) {
      errorResponse(args, new ErrorResponse(Status.BAD_REQUEST, errorFromBindingResult(bindingResult)),
        request, response);
    } else {
      ServiceResponse sr = service.traverse(args.getPath(), args.getUri());
      if (sr instanceof ErrorResponse) {
        errorResponse(args, (ErrorResponse) sr, request, response);
      } else {
        track(request, args, null, null);
        //TODO: log/track data providers that occur is the traverse query result
        TraverseResponse traverseResponse = (TraverseResponse) sr;
        traverseResponse.setVersion(CPathSettings.getInstance().getVersion());
        return traverseResponse;
      }
    }
    return null;
  }

  @ApiOperation(value = "BioPAX graph query.",
    notes = "This is useful for finding connections of bio network elements, such as the " +
    "shortest path between two proteins or the neighborhood for a particular protein state or all states. Graph " +
    "searches consider detailed BioPAX semantics, such as generics, nested complexes, and traverse the graph " +
    "accordingly. We integrate data from multiple databases and consistently normalize Xref, " +
    "EntityReference, Provenance, BioSource, and ControlledVocabulary objects if we are absolutely sure about " +
    "several objects of the same type are equivalent. We do not merge physical entities (states) and processes " +
    "from different sources automatically, as accurately matching and aligning pathways at that level is still an " +
    "open research problem.")
  @RequestMapping("/graph")
  public void graphQuery(@Valid Graph args, BindingResult bindingResult,
                         HttpServletRequest request, HttpServletResponse response)
  {
    //check for binding errors
    if (bindingResult.hasErrors()) {
      errorResponse(args, new ErrorResponse(Status.BAD_REQUEST, errorFromBindingResult(bindingResult)),
        request, response);
      return;
    }

    ServiceResponse result;

    Map<String, String> formatOptions = new HashMap<String, String>();
    if (args.getPattern() != null && args.getPattern().length > 0)
      formatOptions.put("pattern", StringUtils.join(args.getPattern(), ","));

    switch (args.getKind()) {
      case NEIGHBORHOOD:
        result = service.getNeighborhood(args.getFormat(), formatOptions, args.getSource(),
          args.getLimit(), args.getDirection(), args.getOrganism(), args.getDatasource(), args.isSubpw());
        break;
      case PATHSBETWEEN:
        result = service.getPathsBetween(args.getFormat(), formatOptions, args.getSource(),
          args.getLimit(), args.getOrganism(), args.getDatasource(), args.isSubpw());
        break;
      case PATHSFROMTO:
        result = service.getPathsFromTo(args.getFormat(), formatOptions, args.getSource(),
          args.getTarget(), args.getLimit(), args.getOrganism(), args.getDatasource(), args.isSubpw());
        break;
      case COMMONSTREAM:
        result = service.getCommonStream(args.getFormat(), formatOptions, args.getSource(),
          args.getLimit(), args.getDirection(), args.getOrganism(), args.getDatasource(), args.isSubpw());
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

  @ApiOperation(value = "A full-text search in the BioPAX database using Lucene query syntax",
    notes = "Index fields (case-sensitive): uri, keyword, name, pathway, xrefid, datasource, and organism " +
      "can be optionally used in a query string. For example, the 'pathway' field helps find entities and interactions " +
      "by keywords or uris matching their parent pathways'; 'xrefid' helps find objects by direct or nested Xref; " +
      "'keyword' (default search field) aggregates most of BioPAX properties of each element " +
      "and child elements (e.g. a Complex can be found by one of its member's name or EC Number). " +
      "Filters by datasource, organism and BioPAX type can be also used. " +
      "Search can be used to select starting points (seeds) for a graph query (see: '/graph','/traverse','/get').")
  @RequestMapping(value = "/search")
  public SearchResponse search(@Valid Search args, BindingResult bindingResult,
                               HttpServletRequest request, HttpServletResponse response) {
    if (bindingResult.hasErrors()) {
      errorResponse(args, new ErrorResponse(Status.BAD_REQUEST,
        errorFromBindingResult(bindingResult)), request, response);
      return null;
    } else {
      // get results from the service
      ServiceResponse results = service.search(args.getQ(), args.getPage(), args.getType(),
        args.getDatasource(), args.getOrganism());

      if (results instanceof ErrorResponse) {
        errorResponse(args, (ErrorResponse) results, request, response);
        return null;
      } else {
        // log/track one data access event for each data provider listed in the result
        track(request, args, ((SearchResponse) results).getProviders(), null);
        SearchResponse searchResponse = (SearchResponse) results;
        searchResponse.setVersion(CPathSettings.getInstance().getVersion());
        return searchResponse;
      }
    }
  }

}