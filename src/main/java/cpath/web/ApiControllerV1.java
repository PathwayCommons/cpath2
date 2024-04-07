package cpath.web;

import java.util.*;

import cpath.service.ErrorResponse;
import cpath.service.api.Status;
import cpath.web.args.*;
import cpath.service.jaxb.*;

import io.swagger.v3.oas.annotations.Operation;
import org.apache.commons.lang3.StringUtils;

import org.springframework.context.annotation.Profile;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import static org.springframework.http.MediaType.*;

/**
 * cPath2 API v1 (for backward compatibility with existing client apps)
 * @deprecated - migrate to the API v2; see: @{@link ApiControllerV2}
 */
@Profile("web")
@RestController
@Deprecated
public class ApiControllerV1 extends BasicController {
  /*
   * Custom web request parameters bindings are defined in GlobalControllerAdvice class (perhaps not needed anymore...)
   */

  @GetMapping(path = "get",
      produces = {"application/vnd.biopax.rdf+xml", "application/ld+json", "application/json", "application/xml", "text/plain"})
  @Operation(
      summary = "Fetch a BioPAX sub-model by URIs/IDs and optionally convert to another output format (query parameters must be URL-encoded and not too many).",
      description = "Retrieve BioPAX pathways, interactions, physical entities from the db by URIs; " +
          "optionally, convert the result to other <a href='/#formats'>output formats</a>."
  )
  public void fetchQueryGet(@Valid Fetch args, BindingResult bindingResult,
                             HttpServletRequest request, HttpServletResponse response) {
    fetchQuery(args, bindingResult, request, response);
  }

  @PostMapping(path = "get",
      consumes = {APPLICATION_FORM_URLENCODED_VALUE},
      produces = {"application/vnd.biopax.rdf+xml", "application/ld+json", "application/json", "application/xml", "text/plain"})
  @Operation(
    summary = "Fetch a BioPAX sub-model by URIs/IDs and optionally convert to another output format",
    description = "Retrieve pathways/interactions/entities by their BioPAX URIs; " +
        "optionally, convert the result to other <a href='/#formats'>output formats</a>."
  )
  public void fetchQuery(@Valid Fetch args, BindingResult bindingResult,
                         HttpServletRequest request, HttpServletResponse response)
  {
    if(bindingResult.hasErrors()) {
      errorResponse(args, new ErrorResponse(Status.BAD_REQUEST, errorFromBindingResult(bindingResult)),
        request, response);
    } else {
      String[] uris = args.getUri();
      Map<String,String> options = new HashMap<>();
      if(args.getPattern()!=null && args.getPattern().length>0) {
        //use StringUtils.join (not String.join) here due to it is an enum (not char seq.) array!
        options.put("pattern", StringUtils.join(args.getPattern(), ","));
      }
      ServiceResponse result = service.fetch(args.getFormat(), options, args.getSubpw(), uris);
      stringResponse(args, result, request, response);
    }
  }

  @GetMapping(path = "top_pathways",
      produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE})
  @Operation(
      summary = "HTTP GET, search for top pathways.",
      description = "Find root/parent Pathway objects, i.e, ones that are neither 'controlled' " +
          "nor a 'pathwayComponent' of another biological process; trivial pathways are excluded from the results;" +
          " can filter by <a href='/datasources'>datasource</a> and organism."
  )
  public SearchResponse topPathwaysQueryGet(@Valid TopPathways args, BindingResult bindingResult,
                                    HttpServletRequest request, HttpServletResponse response)
  {
    return topPathwaysQuery(args, bindingResult, request, response);
  }

  @PostMapping(path = "top_pathways",
      consumes = {APPLICATION_FORM_URLENCODED_VALUE},
      produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE})
  @Operation(
    summary = "Search for top-level bio pathways.",
    description = "Find root/parent Pathway objects that are neither <code>controlled</code> " +
      "nor a <code>pathwayComponent</code> of another biological process; trivial pathways are excluded from the results;" +
      " can filter by <a href='/datasources'>datasource</a> and organism."
  )
  public SearchResponse topPathwaysQuery(@Valid TopPathways args, BindingResult bindingResult,
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
        audit(request, args, hits.getProviders(), null);
        hits.setVersion(service.settings().getVersion());
        return hits;
      }
    }
  }

  @GetMapping(path = "traverse", produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE})
  @Operation(
      summary = "Access properties of BioPAX elements using graph path expressions",
      description = "To collect specific BioPAX property values, use the following path accessor format: " +
          "InitialClass/property[:filterClass]/[property][:filterClass]... A \"*\" sign after the property " +
          "instructs the path accessor to transitively traverse that property. For example, the following " +
          "path accessor will traverse through all physical entity components a complex, including components " +
          "of nested complexes, if any: Complex/component*/entityReference/xref:UnificationXref. " +
          "The next would list display names of all participants of interactions, which are pathway components " +
          "of a pathway: Pathway/pathwayComponent:Interaction/participant*/displayName. " +
          "Optional restriction ':filterClass' enables limiting the property values to a certain sub-class " +
          "of the object property range. In the first example above, this is used to get only the unification xrefs. " +
          "All the official BioPAX properties as well as additional derived classes and properties, " +
          "such as inverse properties and interfaces that represent anonymous union classes in BioPAX OWL " +
          "can be used in a path accessor."
  )
  public TraverseResponse traverseQueryGet(@Valid Traverse args, BindingResult bindingResult,
                                   HttpServletRequest request, HttpServletResponse response) {
    return traverseQuery(args, bindingResult, request, response);
  }

  @PostMapping(path = "traverse",
      consumes = {APPLICATION_FORM_URLENCODED_VALUE},
      produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE})
  @Operation(
    summary = "Access properties of BioPAX elements using graph path expressions (xpath-like).",
    description = "To collect specific BioPAX property values, use the following path accessor format: " +
      "InitialClass/property[:filterClass]/[property][:filterClass]... A \"*\" sign after the property " +
      "instructs the path accessor to transitively traverse that property. For example, the following " +
      "path accessor will traverse through all physical entity components a complex, including components " +
      "of nested complexes, if any: Complex/component*/entityReference/xref:UnificationXref. " +
      "The next would list display names of all participants of interactions, which are pathway components " +
      "of a pathway: Pathway/pathwayComponent:Interaction/participant*/displayName. " +
      "Optional restriction ':filterClass' enables limiting the property values to a certain sub-class " +
      "of the object property range. In the first example above, this is used to get only the unification xrefs. " +
      "All the official BioPAX properties as well as additional derived classes and properties, " +
      "such as inverse properties and interfaces that represent anonymous union classes in BioPAX OWL " +
      "can be used in a path accessor."
  )
  public TraverseResponse traverseQuery(@Valid Traverse args, BindingResult bindingResult,
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
        audit(request, args, null, null);
        TraverseResponse traverseResponse = (TraverseResponse) sr;
        traverseResponse.setVersion(service.settings().getVersion());
        return traverseResponse;
      }
    }
    return null;
  }

  @GetMapping(path = "graph",
      produces = {"application/vnd.biopax.rdf+xml", "application/ld+json", "application/json", "application/xml", "text/plain"})
  @Operation(
      summary = "BioPAX Graph Query and optional converter to another output format (parameters must be URL-encoded and not too many).",
      description = "Find connections of bio network elements, such as the shortest path between " +
          "two proteins or the neighborhood for a particular protein state or all states. " +
          "Optionally, convert the result to other <a href='/#formats'>output formats</a>." +
          "Graph searches consider detailed BioPAX semantics, such as generics, nested complexes, " +
          "and traverse the graph accordingly."
  )
  public void graphQueryGet(@Valid Graph args, BindingResult bindingResult,
                            HttpServletRequest request, HttpServletResponse response)
  {
    graphQuery(args, bindingResult, request, response);
  }


  @PostMapping(path = "graph",
      consumes = {APPLICATION_FORM_URLENCODED_VALUE},
      produces = {"application/vnd.biopax.rdf+xml", "application/ld+json", "application/json", "application/xml", "text/plain"})
  @Operation(
    summary = "A BioPAX graph query and optional converter to another output format.",
    description = "Find connections of bio network elements, such as the shortest path between " +
      "two proteins or the neighborhood for a particular protein state or all states. " +
      "Optionally, convert the result to other <a href='/#formats'>output formats</a>." +
      "Graph searches consider detailed BioPAX semantics, such as generics, nested complexes, " +
      "and traverse the graph accordingly."
  )
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

    Map<String,String> formatOptions = new HashMap<>();
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
        result = service.getPathsFromTo(args.getFormat(), formatOptions, args.getSource(), args.getTarget(),
            args.getLimitType(), args.getLimit(), args.getOrganism(), args.getDatasource(), args.getSubpw());
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

  @GetMapping(path = "search", produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE})
  @Operation(
      summary = "Full-text search in the BioPAX database with Lucene query syntax",
      description = """
        <p>
        The index field names are: <var>uri, keyword, name, pathway, xrefid, datasource, organism</var>.
        E.g., <var>keyword</var> is the default aggregate field that includes most of BioPAX element's properties
        and nested properties (e.g. a Complex can be found by one of its member's names or EC Number).
        Search results, specifically the URIs, can be starting point for the graph, get, traverse queries.
        Search strings are case-insensitive, except for <var>xrefid, uri</var>, or when it's enclosed in quotes.
        </p>
        <p>
        Returns an ordered list of hits (<var>maxHitsPerPage</var> is configured on the server) as JSON or
        <a href="/help/schema">XML</a> depending on 'Accept: application/json' or
        'Accept: application/xml' request header.
        </p>
        """
  )
  public SearchResponse searchQueryGet(@Valid Search args, BindingResult bindingResult,
                               HttpServletRequest request, HttpServletResponse response)
  {
    return searchQuery(args, bindingResult, request, response);
  }

  @PostMapping(path = "search",
      consumes = {APPLICATION_FORM_URLENCODED_VALUE},
      produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE})
  @Operation(
    summary = "Full-text search the BioPAX model using Lucene query syntax",
    description = """
        <p>
        The index field names are: <var>uri, keyword, name, pathway, xrefid, datasource, organism</var>.
        E.g., <var>keyword</var> is the default aggregate field that includes most of BioPAX element's properties
        and nested properties (e.g. a Complex can be found by one of its member's names or EC Number).
        Search results, specifically the URIs, can be starting point for the graph, get, traverse queries.
        Search strings are case insensitive, except for <var>xrefid, uri</var>, or when it's enclosed in quotes.
        </p>
        <p>
        Returns an ordered list of hits (<var>maxHitsPerPage</var> is configured on the server) as JSON or 
        <a href="/help/schema">XML</a> depending on 'Accept: application/json' or 
        'Accept: application/xml' request header.
        </p>
        """
  )
  public SearchResponse searchQuery(@Valid Search args, BindingResult bindingResult,
                               HttpServletRequest request, HttpServletResponse response)
  {
    SearchResponse searchResponse = null;

    if(bindingResult.hasErrors()) {
      errorResponse(args, new ErrorResponse(Status.BAD_REQUEST,
        errorFromBindingResult(bindingResult)), request, response);
    } else {
      // get results from the service
      ServiceResponse results = service.search(args.getQ(), args.getPage(), args.getBiopaxClass(),
        args.getDatasource(), args.getOrganism());

      if(results instanceof ErrorResponse) {
        errorResponse(args, (ErrorResponse) results, request, response);
      } else if(results != null) {
        // log data access event for each data provider listed in the result
        audit(request, args, ((SearchResponse)results).getProviders(), null);
        searchResponse = (SearchResponse) results;
        searchResponse.setVersion(service.settings().getVersion());
      }
    }

    return searchResponse;
  }

}