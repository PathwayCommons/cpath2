package cpath.client;

import cpath.client.util.BioPAXHttpMessageConverter;
import cpath.client.util.CPathException;
import cpath.client.util.ServiceResponseHttpMessageConverter;
import cpath.service.Cmd;
import cpath.service.CmdArgs;
import cpath.service.GraphType;
import cpath.service.OutputFormat;
import cpath.service.jaxb.*;

import org.apache.commons.lang.StringUtils;
import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.io.BioPAXIOHandler;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.UnknownHttpStatusCodeException;

import java.util.*;


/**
 * CPath2 Web Service Client. 
 * 
 */
public final class CPath2Client
{
	private static final Logger LOGGER = LoggerFactory.getLogger(CPath2Client.class);
	
	// one can set the JVM property: -DcPath2Url="http://some_URL"
	public static final String JVM_PROPERTY_ENDPOINT_URL = "cPath2Url";	
	public static final String DEFAULT_ENDPOINT_URL = "http://purl.org/pc2/current/";

	/**
	 * This is an <em>equivalent</em> to {@link Direction}
	 * enumeration, and is defined here for convenience (i.e.,
	 * to free all users of this web service client from 
	 * depending on paxtools-query module at runtime.)
	 * 
	 */
	public static enum Direction
    {
		UPSTREAM, DOWNSTREAM, BOTHSTREAM;
    }
	
	
	final RestTemplate restTemplate;
	
	private String endPointURL;
	private Integer page = 0;
    private Integer graphQueryLimit = 1;
    private Collection<String> organisms = new HashSet<String>();
    private Collection<String> dataSources = new HashSet<String>();
    private String type = null;
    private String path = null;
    private Direction direction = null;

	/**
	 * Option to merge equivalent interactions (with same participants) in the result model.
	 */
	private boolean mergeEquivalentInteractions = false;

	// suppress using constructors in favor of static factories
    private CPath2Client() {
     	// create a new REST template
     	restTemplate = new RestTemplate(); //custom message converters will be added there
    }
    
    
    /**
     * Default static factory, initializes the class using
     * {@link org.biopax.paxtools.io.SimpleIOHandler}
     */
    public static CPath2Client newInstance() {
    	return newInstance(new SimpleIOHandler());
    }

    
    /**
     * Static factory.
     * @param bioPAXIOHandler BioPAXIOHandler for reading BioPAX Models
     */
    public static CPath2Client newInstance(BioPAXIOHandler bioPAXIOHandler) {
    	CPath2Client client = new CPath2Client(); 
    	
     	// add custom cPath2 XML message converter as the first one (accepts 'application/xml' content type)
    	// because one of existing/default msg converters, XML root element based jaxb2, does not work for ServiceResponce types...
    	client.restTemplate.getMessageConverters().add(0, new ServiceResponseHttpMessageConverter());
    	// add BioPAX http message converter
        client.restTemplate.getMessageConverters().add(1, new BioPAXHttpMessageConverter(bioPAXIOHandler));
    	
    	// init the server PROVIDER_URL
    	client.endPointURL = System.getProperty(JVM_PROPERTY_ENDPOINT_URL, DEFAULT_ENDPOINT_URL);
    	
    	assert client.endPointURL != null :  "BUG: cpath2 PROVIDER_URL is not defined";
    	
    	// find the actual URL (or at least the first one that works via POST)
    	String origUrl = client.endPointURL;
    	while(true) {
    		ResponseEntity<String> re = client.restTemplate
    			.exchange(client.endPointURL, HttpMethod.HEAD, null, String.class);  		
    		
    		if(re.getStatusCode().equals(HttpStatus.FOUND)
    			|| re.getStatusCode().equals(HttpStatus.MOVED_PERMANENTLY)) {
    			client.endPointURL = re.getHeaders().getLocation().toString();
    			LOGGER.info("Found new location: " + client.endPointURL 
        				+ "; " + re.getStatusCode());
    		}
    		else if(re.getStatusCode().equals(HttpStatus.OK)) {
    			LOGGER.info("Success: " + client.endPointURL 
    				+ "; " + re.getStatusCode());
    			break; //exit the infinite loop
    		}
    		else {
    			throw new RuntimeException("HTTP POST failed " +
    				"after the client was redirected " +
    				"from " + origUrl +	" to " + client.endPointURL 
    				+ " (status: " + re.getStatusCode() + ")");
    		}
    	}
    	
    	
    	return client;
    }

	/**
	 * Executes a 'get' and 'graph' type cPath2 
	 * Web Service API PROVIDER_URL query and returns 
	 * the resulting sub-model (sub-network) as String.
	 * 
	 * @see #queryGet(Collection)
	 * @see #queryNeighborhood(Collection)
	 * @see #queryCommonStream(Collection)
	 * @see #queryPathsFromTo(Collection, Collection)
	 * @see #queryPathsBetween(Collection)
	 * 
	 * 
	 * @param urlQuery
	 * @param outputFormat default is {@link OutputFormat#BIOPAX}
	 * @return data in the requested format
	 * @throws CPathException if there is no results or another problem
	 * 
	 * @deprecated good for testing; otherwise is but low-level and error-prone method
	 */
	public String executeQuery(final String urlQuery, final OutputFormat outputFormat)
			throws CPathException 
	{		
		final String q = (outputFormat == null) ? urlQuery : urlQuery + "&"
				+ CmdArgs.format + "=" + outputFormat;

		return doGet(q, String.class);
	}
	
	
	/**
	 * Sends a HTTP POST request to the server.
	 * 
	 * This is a lower-level query method than 
	 * others provided by this class, such as 
	 * {@link #getNeighborhoodAsString(Collection, OutputFormat)}, etc.
	 * 
	 * @param command cpath2 web service command
	 * @param respClass result class (e.g., String.class, Model)
	 * @param request query object, e.g., a Map; use {@link #buildRequest(Cmd, GraphType, Collection, Collection, OutputFormat)} to create one.
	 * @return
	 * @throws CPathException
	 */
	public <T> T doPost(Cmd command, Class<T> respClass, Object request)
		throws CPathException 
	{
		final String url = endPointURL + command;
		
		try {
			return restTemplate.postForObject(url, request, respClass);
		} catch (UnknownHttpStatusCodeException e) {
			if (e.getRawStatusCode() == 460) {
				return null; //empty result
			} else
				throw new CPathException(url + " and " + request.toString(), e);
		} catch (RestClientException e) {
			throw new CPathException(url + " and " + request.toString(), e);
		}
	}
    
	
	private <T> T doGet(String url, Class<T> respClass)
		throws CPathException 
	{
		try {
			return restTemplate.getForObject(url, respClass);
		} catch (UnknownHttpStatusCodeException e) {
			if (e.getRawStatusCode() == 460) {
				return null; //empty result
			} else
				throw new CPathException(url, e);
		} catch (RestClientException e) {
			throw new CPathException(url, e);
		}
	}
	
    
    /**
     * Full text search. 
     * Used primarily to locate starting points 
     * for traversals.
     * 
     * Retrieves one "page" of full-text search results
     * (the page number is given by {@link #getPage()} method) - 
     * ordered by Lucene score list of (BioPAX element) hits 
     * matched the query expression and passed current filters.
     * 
     * See the cPath2 web service online description 
     * for the list of available index field names and filter 
     * values (i.e., officially supported data sources and organisms).
     * 
     * If no keywords or "*" is used, 
     * then at least BioPAX type filter must be set using 
     * {@link #setType(String)}. 
     *
     * @see #setType(String)
     * @see #setPage(Integer)
     * @see #setDataSources(Collection)
     * @see #setOrganisms(Collection)
     *
     * @param keywords names, identifiers, or Lucene queries (will be joint with 'OR')
     * @return
     * @throws CPathException when the WEB API gives an error
     */
    public SearchResponse search(String... keywords) 
    		throws CPathException 
    {
    	final String kw = (keywords == null || keywords.length == 0)
    			? "*" : join("", Arrays.asList(keywords), " ");
    	
    	String url = endPointURL + Cmd.SEARCH + "?" 
            	+ CmdArgs.q + "=" + kw // spaces means 'OR'
                + (getPage() > 0 ? "&" + CmdArgs.page + "=" + getPage() : "")
                + (getDataSources().isEmpty() ? "" : "&" + join(CmdArgs.datasource + "=", getDataSources(), "&"))
                + (getOrganisms().isEmpty() ? "" : "&" + join(CmdArgs.organism + "=", getOrganisms(), "&"))
                + (getType() != null ? "&" + CmdArgs.type + "=" + getType() : "");
    	
    	return doGet(url, SearchResponse.class);
    }

    
    /**
     * Full text search - grab all hits at once.
     * 
     * Retrieves ALL or first N pages of hits that match the  
     * query and pass current type, organism and datasource 
     * filters (if specified). If no keywords or "*" is used, 
     * then at least BioPAX type filter must be set using 
     * {@link #setType(String)}. 
     * 
     * This method is to quickly extract all identifiers, names, 
     * parent pathways (URIs) of a sub-class of BioPAX elements
     * rather than to find the best match (top-scored hits of a 
     * specific search).
     * 
     * @see #setType(String) - filter by BioPAX type
     * @see #setPage(Integer) - if >0, gets search hits from 0 to this page number.
     * @see #setDataSources(Collection)
     * @see #setOrganisms(Collection)
     * @param keywords
     * @return
     */
    public List<SearchHit> findAll(String... keywords) {
    	List<SearchHit> hits = new ArrayList<SearchHit>();

    	int numPages = getPage(); //0 (all) if wasn't set by user
    	int page = 0;
    	SearchResponse res;
    	do {
    		setPage(page);
    		
    		try {
				res = search(keywords);
			} catch (CPathException e) {
				break;
			}
    		
    		if(!res.isEmpty())
    			hits.addAll(res.getSearchHit());
    		else //should not happen (cpath2 returns error status when empty result)
    			break;
    		
    		page++;
    		
    	} while(numPages <= 0 || page < numPages);
    	
    	return hits;
    }
    
    
    /**
     * Retrieves details regarding one or more records, such as pathway,
     * interaction or physical entity. For example, get the complete
     * Apoptosis pathway from Reactome.
     *
     * @param id a BioPAX element URI or standard identifier (UniProt, NCBI Gene,..)
     * @return BioPAX model containing the requested element
     * @throws CPathException 
     * 
     */
    public Model get(String id) throws CPathException {
		return get(Collections.singleton(id));
    }


    /**
	 * Retrieves the model using the given command and parameters object.
	 * 
	 * @param command that returns a model (i.e., /get or /graph)
	 * @param request web parameters map
	 * @return model
	 * @throws CPathException 
	 * 
	 */
	private Model getModel(Cmd command, Object request) 
			throws CPathException
	{
		Model model = doPost(command, Model.class, request);
		if (mergeEquivalentInteractions)
			ModelUtils.mergeEquivalentInteractions(model);
		
		return model;
	}
 
	
    /**
     * Retrieves a sub-model based on one or more pathways,
     * interactions, or physical entities. For example, one can 
     * retrieve the complete Reactome's Apoptosis pathway model
     * (by its original URI), or - get xrefs (by identifiers or gene names).
     *
     * @param ids a set of BioPAX element IDs
     * @return BioPAX model containing the requested elements and child elements. 
     * @throws CPathException 
     */
    public Model get(Collection<String> ids) throws CPathException 
    {
		return getModel(Cmd.GET, 
			buildRequest(Cmd.GET, null, ids, null, null));
    }

    
    /**
     * Retrieves a sub-model based on one or more pathways,
     * interactions, or physical entities. For example, one can 
     * retrieve the complete Reactome's Apoptosis pathway model.
     * The result is returned in the requested text format.
     * 
     * @param ids identifiers of URIs
     * @param outputFormat format (if null, - BioPAX is the default)
     * @return
     * @throws CPathException 
     */
    public String getAsString(Collection<String> ids, final OutputFormat outputFormat) 
    		throws CPathException 
    {
    	return doPost(Cmd.GET, String.class, 
    		buildRequest(Cmd.GET, null, ids, null, outputFormat));
    }

    
    /**
     * Builds a cpath2 web query parameters objects.
     * 
     * This is to build a request parameters object for
     * the {@link #doPost(Cmd, Class, Object)} method.
     * 
     * @param command
     * @param graphType
     * @param sources
     * @param targets
     * @param outputFormat
     * @return
     */
    public Object buildRequest(Cmd command, GraphType graphType, Collection<String> sources, 
    		Collection<String> targets, OutputFormat outputFormat) 
    {	
    	MultiValueMap<String, String> request = new LinkedMultiValueMap<String, String>();
    	
    	switch(command) {
    	case GRAPH:
    		if(outputFormat == null) 
    			outputFormat = OutputFormat.BIOPAX;
			request.add(CmdArgs.format.name(), outputFormat.name());
    		
    		// common options for all graph commands
    		request.add(CmdArgs.kind.name(), graphType.name());
    		request.add(CmdArgs.limit.name(), graphQueryLimit.toString());
    		request.put(CmdArgs.source.name(), new ArrayList<String>(sources));
    		request.put(CmdArgs.organism.name(), new ArrayList<String>(organisms));
    		request.put(CmdArgs.datasource.name(), new ArrayList<String>(dataSources));
    		
    		switch(graphType) {
    			case COMMONSTREAM:
    		    	if(direction != null) {
    		    		if (direction == Direction.BOTHSTREAM)
    		    			throw new IllegalArgumentException(
    		    				"Direction of common-stream query should be either upstream or downstream.");
    		    		else
    		    			request.add(CmdArgs.direction.name(), direction.name());
    		    	}
    				break;
    			case PATHSBETWEEN:
    				break;
    			case PATHSFROMTO:
    		    	request.put(CmdArgs.target.name(), new ArrayList<String>(targets));    		    	
    				break;
    			case NEIGHBORHOOD:
    			default:
    		    	if(direction != null) 
    		    		request.add(CmdArgs.direction.name(), direction.name());
    				break;
    		}
    		break;
    	case TRAVERSE:
    		request.put(CmdArgs.uri.name(), new ArrayList<String>(sources));
        	request.add(CmdArgs.path.name(), path);
    		break;
    	case GET:
    	default: //GET is the default query
    		if(outputFormat == null) 
    			outputFormat = OutputFormat.BIOPAX;
			request.add(CmdArgs.format.name(), outputFormat.name());
			
    		request.put(CmdArgs.uri.name(), new ArrayList<String>(sources));
    		break;
    	}
    	
    	return request;
	}

    
    /**
	 *  Finds paths between a given source set of objects. The source set may contain Xref,
	 *  EntityReference, and/or PhysicalEntity objects.
	 *
	 * @param sourceSet set of xrefs, entity references, or physical entities
	 * @return a BioPAX model that contains the path(s).
     * @throws CPathException 
	 */
	public Model getPathsBetween(Collection<String> sourceSet) throws CPathException
	{
		return getModel(Cmd.GRAPH, 
			buildRequest(Cmd.GRAPH, GraphType.PATHSBETWEEN, sourceSet, null, null));
	}

	
	/**
	 *  Finds paths between a given source set of objects
	 *  and returns the sub-model in the requested text format. 
	 * 
	 * @param sourceSet
	 * @param outputFormat the default is BIOPAX (when null)
	 * @return
	 * @throws CPathException 
	 */
    public String getPathsBetweenAsString(Collection<String> sourceSet, 
    		final OutputFormat outputFormat) throws CPathException 
    {
    	return doPost(Cmd.GRAPH, String.class, 
    		buildRequest(Cmd.GRAPH, GraphType.PATHSBETWEEN, sourceSet, null, null));
    }
	
	
	/**
	 *  Finds paths from a given source set of objects to a given target set of objects. 
	 *  Source and target sets may contain Xref, EntityReference, and/or PhysicalEntity objects.
	 *
	 * @param sourceSet set of xrefs, entity references, or physical entities
	 * @param targetSet set of xrefs, entity references, or physical entities
	 * @return a BioPAX model that contains the path(s).
	 * @throws CPathException 
	 */
	public Model getPathsFromTo(Collection<String> sourceSet, Collection<String> targetSet) 
			throws CPathException 
	{
		return getModel(Cmd.GRAPH, 
			buildRequest(Cmd.GRAPH, GraphType.PATHSFROMTO, sourceSet, targetSet, null));
	}

	
	/**
	 * Finds paths from a given source set of objects to 
	 * a given target set of objects and returns the result 
	 * (string) in the given text format.
	 * 
	 * @param sourceSet
	 * @param targetSet
	 * @param outputFormat the default is BIOPAX (when null)
	 * @return
	 * @throws CPathException 
	 */
    public String getPathsFromToAsString(Collection<String> sourceSet, Collection<String> targetSet, 
    		OutputFormat outputFormat) throws CPathException 
    {
    	return doPost(Cmd.GRAPH, String.class, 
    		buildRequest(Cmd.GRAPH, GraphType.PATHSFROMTO, sourceSet, targetSet, outputFormat));
    }

	
	/**
	 * Searches directed paths from and/or to the given source set of entities, 
	 * in the specified search limit.
	 *
	 * @param sourceSet Set of source physical entities
	 * @return BioPAX model representing the neighborhood.
	 * @throws CPathException 
	 */
	public Model getNeighborhood(Collection<String> sourceSet) 
			throws CPathException
	{
		return getModel(Cmd.GRAPH, buildRequest(Cmd.GRAPH, GraphType.NEIGHBORHOOD, sourceSet, null, null));
	}

	
	/**
	 * A nearest neighborhood biopax query that returns the result
	 * in the specified text format.
	 * 
	 * @param sourceSet
	 * @param outputFormat the default is BIOPAX (when null)
	 * @return
	 * @throws CPathException 
	 */
    public String getNeighborhoodAsString(Collection<String> sourceSet, 
    		final OutputFormat outputFormat) throws CPathException 
    {
    	return doPost(Cmd.GRAPH, String.class, 
    		buildRequest(Cmd.GRAPH, GraphType.NEIGHBORHOOD, sourceSet, null, outputFormat));
    }
	
	
	/**
	 * This query searches for the common upstream (common regulators) or
	 * common downstream (common targets) objects of the given source set.
	 *
	 * @see #setDirection(Direction) (only upstream or downstream)
	 *
	 * @param sourceSet set of physical entities
	 * @return a BioPAX model that contains the common stream
	 * @throws CPathException 
	 */
	public Model getCommonStream(Collection<String> sourceSet) 
			throws CPathException
	{
		return getModel(Cmd.GRAPH, 
			buildRequest(Cmd.GRAPH, GraphType.COMMONSTREAM, sourceSet, null, null));
	}

	
	/**
	 * Searches for the common upstream (common regulators) or
	 * common downstream (common targets) biopax objects of 
	 * the given source set and returns the result
	 * in the given text format.
	 * 
	 * @param sourceSet
	 * @param outputFormat the default is BIOPAX (when null)
	 * @return
	 * @throws CPathException 
	 */
    public String getCommonStreamAsString(Collection<String> sourceSet, 
    		final OutputFormat outputFormat) throws CPathException 
    {
    	return doPost(Cmd.GRAPH, String.class,
    		buildRequest(Cmd.GRAPH, GraphType.COMMONSTREAM, sourceSet, null, outputFormat));
    }
	
    /**
     * Gets the list of top (root) pathways 
     * (in the same xml format used by the full-text search commands)
     * 
     * @return
     */
    public SearchResponse getTopPathways() {
    	
    	SearchResponse resp = restTemplate.getForObject(endPointURL 
        		+ Cmd.TOP_PATHWAYS, SearchResponse.class);
    	
    	Collections.sort(resp.getSearchHit(), new Comparator<SearchHit>() {
			@Override
			public int compare(SearchHit h1, SearchHit h2) {
				return h1.toString().compareTo(h2.toString());
			}
		});    	
    	
    	return resp;
    }    

    
    /**
     * Gets the values (for data types) or URIs (objects) from a 
     * BioPAX property path starting from each of specified URIs.
     * 
     * @param uris
     * @return
     * @throws CPathException when there was returned a HTTP error
     */
    public TraverseResponse traverse(Collection<String> uris) 
    		throws CPathException 
    {
    	return doPost(Cmd.TRAVERSE, TraverseResponse.class, 
    			buildRequest(Cmd.TRAVERSE, null, uris, null, null));
    }

    
    /**
     * Joins the collection of strings into one string 
     * using the prefix and delimiter.
     * 
     * @param prefix 
     * @param strings
     * @param delimiter
     * @return
     */
    private String join(String prefix, Collection<String> strings, String delimiter) {
        List<String> prefixed = new ArrayList<String>();

       	for(String s: strings)
       		prefixed.add(prefix + s);

        return StringUtils.join(prefixed, delimiter);
    }
    
    
    /**
     * The WEB Service PROVIDER_URL prefix.
     * 
     * @return the end point PROVIDER_URL as a string
     */
    public String getEndPointURL() {
        return endPointURL;
    }

    
    /**
     * @see #getEndPointURL()
     * @param endPointURL the end point PROVIDER_URL as a string
     */
    public void setEndPointURL(String endPointURL) {
        this.endPointURL = endPointURL;
    }

    
    /**
     * Pathway Commons returns no more than N (e.g., 1000) search hits per request.
     * You can request results beyond the first N by using the page parameter.
     * Default is 0. Total number of result pages (P) can be calculated using the first
     * page SearchResponse attributes as follows: P = INT[ (numHits - 1)/numHitsPerPage + 1 ]
     *
     * @see #search(Collection)
     *
     * @return the page number
     */
    public Integer getPage() {
        return page;
    }

    
    /**
     * @see #getPage()
     * @param page page number ()
     */
    public void setPage(Integer page) {
    	if(page >= 0)
    		this.page = page;
    	else 
    		throw new IllegalArgumentException("Negative page numbers are not supported");
    }

    
    /**
     * Graph query search distance limit (default = 1).
     *
     * @see #getNeighborhood(Collection)
     * @see #getCommonStream(Collection)
     * @see #getPathsBetween(Collection)
     *
     * @return distance limit.
     */
    public Integer getGraphQueryLimit() {
        return graphQueryLimit;
    }

    
    /**
     * @see #getGraphQueryLimit()
     *
     * @param graphQueryLimit graph distance limit
     */
    public void setGraphQueryLimit(Integer graphQueryLimit) {
        this.graphQueryLimit = graphQueryLimit;
    }

    
    /**
     * BioPAX class filter for find() method.
     *
     * @see #search(String)
     * @see #findEntity(String)
     *
     * @return BioPAX L3 Class simple name
     */
    public String getType() {
        return type;
    }

    
    /**
     * @see #getType()
     *
     * @param type a BioPAX L3 Class
     */
    public void setType(String type) {
        this.type = type;
    }

    
    /**
     * Organism filter for find(). Multiple organism filters are allowed per query.
     *
     * @see #search(String)
     * @see #findEntity(String)
     *
     * @return set of strings representing organisms.
     */
    public Collection<String> getOrganisms() {
        return organisms;
    }

    
    /**
     * @see #getOrganisms()
     *
     * @param organisms set of strings representing organisms.
     */
    public void setOrganisms(Collection<String> organisms) {
        this.organisms = organisms;
    }

    
    /**
     * Data source filter for find(). Multiple data source filters are allowed per query.
     *
     * @see #search(String)
     * @see #findEntity(String)
     *
     * @return data sources as strings
     */
    public Collection<String> getDataSources() {
        return dataSources;
    }

    
    /**
     * @see #getDataSources()
     *
     * @param dataSources data sources as strings
     */
    public void setDataSources(Collection<String> dataSources) {
        this.dataSources = dataSources;
    }

    
    /**
     * @see #getType()
     * @see #setType(String)
     * @return valid values for the BioPAX type parameter.
     */
    public Collection<String> getValidTypes() {
    	Help help = restTemplate.getForObject(endPointURL + "help/types", Help.class);
    	return parseHelpSimple(help).keySet();
    }

    
    private Map<String, String> parseHelpSimple(Help help) {
        Map<String,String> types = new TreeMap<String,String>();
    	for(Help h : help.getMembers()) {
    		String title = (h.getTitle() != null) 
    			? h.getTitle().toUpperCase() : h.getId();
    		types.put(h.getId(), title);
    	}
    	return types;
    }
    
	/**
	 * Gets the BioPAX property path (current value).
	 * @see #traverse(Collection)
	 * 
	 * @return
	 */
	public String getPath() {
		return path;
	}

	
	/**
	 * @see #getPath()
	 * @see #traverse(Collection)
	 * 
	 * @param path
	 */
	public void setPath(String path) {
		this.path = path;
	}
    
		
	/**
	 * Graph query direction (depends on the graph query type)
	 * 
	 * @return the direction
	 */
	public Direction getDirection() {
		return direction;
	}


	/**
	 * @see #getDirection()
	 * @param direction
	 */
	public void setDirection(Direction direction) {
		this.direction = direction;
	}

	/**
	 * Checks the option to merge equivalent interactions in the result model.
	 * @return true if merging equivalent interactions
	 */
	public boolean isMergeEquivalentInteractions()
	{
		return mergeEquivalentInteractions;
	}

	/**
	 * Sets the option to merge equivalent interactions in the result model.
	 * @param mergeEquivalentInteractions option
	 */
	public void setMergeEquivalentInteractions(boolean mergeEquivalentInteractions)
	{
		this.mergeEquivalentInteractions = mergeEquivalentInteractions;
	}
}
