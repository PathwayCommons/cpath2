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
import org.biopax.paxtools.io.BioPAXIOHandler;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.Model;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * CPath2 Client (read as: CPath Squared). 
 * 
 * Development CPath2 WEB API demo is http://awabi.cbio.mskcc.org/pc2-demo/, 
 * and the released one was http://www.pathwaycommons.org/pc2-demo/
 * 
 * For "/get" and "/graph" queries, this client 
 * returns data in BioPAX L3 format only (or - error).
 * But BioPAX can be converted to other formats on the client side
 * (e.g., using converters provided by Paxtools)
 * 
 * TODO add support for other output formats that '/get','/graph' can return (at least - for BINARY_SIF)
 */
public final class CPath2Client
{
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
	
	
	private final RestTemplate restTemplate;
	
	private String endPointURL;
	private Integer page = 0;
    private Integer graphQueryLimit = 1;
    private Collection<String> organisms = new HashSet<String>();
    private Collection<String> dataSources = new HashSet<String>();
    private String type = null;
    private String path = null;
    private Direction direction = null;
    
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
    	
    	// Remove default message converters
//    	client.restTemplate.getMessageConverters().clear();
    	
     	// add custom cPath2 XML message converter as the first one (accepts 'application/xml' content type)
    	// because one of existing/default msg converters, XML root element based jaxb2, does not work for ServiceResponce types...
    	client.restTemplate.getMessageConverters().add(0, new ServiceResponseHttpMessageConverter());
    	// add BioPAX http message converter
        client.restTemplate.getMessageConverters().add(1, new BioPAXHttpMessageConverter(bioPAXIOHandler));
    	
    	// init the server PROVIDER_URL
    	client.endPointURL = System.getProperty(JVM_PROPERTY_ENDPOINT_URL, DEFAULT_ENDPOINT_URL);
    	assert client.endPointURL != null :  "BUG: cpath2 PROVIDER_URL is not defined";
    	
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
	 * @param url
	 * @param outputFormat default is {@link OutputFormat#BIOPAX}
	 * @return data in the requested format
	 * @throws CPathException if there is no results or another problem
	 */
	public String executeQuery(final String url, final OutputFormat outputFormat) 
	throws CPathException 
	{
		final String q = (outputFormat == null)
			? url : url + "&" + CmdArgs.format + "=" + outputFormat;
		
		try {
			return restTemplate.getForObject(q, String.class);
		} catch (Exception e) {
			throw new CPathException(q, e);
		}
		
	}
    
    
    /**
     * Full text search. 
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
    	
        try {
			return restTemplate.getForObject(url, SearchResponse.class);
		} catch (RestClientException e) {
			throw new CPathException(url, e);
		}

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
    	final String kw = (keywords == null || keywords.length == 0)
    			? "*" : join("", Arrays.asList(keywords), " ");
    	int numPages = getPage();
    	int page = 0;
    	SearchResponse res;
    	do {
    		setPage(page);
    		try {
				res = search(kw);
			} catch (CPathException e) {
				break; //no result or error
			}
    		if(!res.isEmpty())
    			hits.addAll(res.getSearchHit());
    		else //should not happen (cpath2 returns error status when empty result)
    			break; 
    	} while(numPages < 1 || ++page < numPages);
    	
    	return hits;
    }
    
    
    /**
     * Retrieves details regarding one or more records, such as pathway,
     * interaction or physical entity. For example, get the complete
     * Apoptosis pathway from Reactome.
     *
     * @param id a BioPAX element ID
     * @return BioPAX model containing the requested element
     */
    public Model get(String id) {
        return get(Collections.singleton(id));
    }

    
    /**
     * Builds a <em>get</em> BioPAX (default format) 
     * by URI(s) query PROVIDER_URL string.
     * 
     * @param ids
     * @return
     */
    public String queryGet(Collection<String> ids) {
        return endPointURL + Cmd.GET + "?" 
        	+ join(CmdArgs.uri + "=" , ids, "&");
    }
    
    
    /**
     * Retrieves details regarding one or more records, such as pathway,
     * interaction or physical entity. For example, get the complete
     * Apoptosis pathway from Reactome.
     *
     * @param ids a set of BioPAX element IDs
     * @return BioPAX model containing the requested element
     */
    public Model get(Collection<String> ids) {
        String url = queryGet(ids);
        return restTemplate.getForObject(url, Model.class);
    }

    
    /**
     * Builds a 'PATHS BETWEEN' BioPAX <em>graph</em> query PROVIDER_URL string.
     * 
     * @param sourceSet
     * @return
     */
    public String queryPathsBetween(Collection<String> sourceSet) {
    	return endPointURL + Cmd.GRAPH + "?" + CmdArgs.kind + "=" +
    		GraphType.PATHSBETWEEN.name().toLowerCase() + "&"
    		+ join(CmdArgs.source + "=", sourceSet, "&") + "&"
    		+ CmdArgs.limit + "=" + graphQueryLimit;
    }  
    
    
    /**
	 *  Finds paths between a given source set of objects. The source set may contain Xref,
	 *  EntityReference, and/or PhysicalEntity objects.
	 *
	 * @param sourceSet set of xrefs, entity references, or physical entities
	 * @return a BioPAX model that contains the path(s).
	 */
	public Model getPathsBetween(Collection<String> sourceSet)
	{
		String url = queryPathsBetween(sourceSet);
		return restTemplate.getForObject(url, Model.class);
	}

	
	/**
	 * Builds a 'PATHS FROM TO' <em>graph</em> query PROVIDER_URL string.
	 * 
	 * @param sourceSet
	 * @param targetSet
	 * @return
	 */
	public String queryPathsFromTo(Collection<String> sourceSet, Collection<String> targetSet)
	{
		return endPointURL + Cmd.GRAPH + "?" + CmdArgs.kind + "=" +
			GraphType.PATHSFROMTO.name().toLowerCase() + "&"
			+ join(CmdArgs.source + "=", sourceSet, "&") + "&"
			+ join(CmdArgs.target + "=", targetSet, "&") + "&"
			+ CmdArgs.limit + "=" + graphQueryLimit;
	}
	
	
	/**
	 *  Finds paths from a given source set of objects to a given target set of objects. 
	 *  Source and target sets may contain Xref, EntityReference, and/or PhysicalEntity objects.
	 *
	 * @param sourceSet set of xrefs, entity references, or physical entities
	 * @param targetSet set of xrefs, entity references, or physical entities
	 * @return a BioPAX model that contains the path(s).
	 */
	public Model getPathsFromTo(Collection<String> sourceSet, Collection<String> targetSet)
	{
		String url = queryPathsFromTo(sourceSet, targetSet);
		return restTemplate.getForObject(url, Model.class);
	}


	/**
	 * Builds a 'NEIGHBORHOOD' BioPAX <em>graph</em> query PROVIDER_URL string.
	 * 
	 * @param sourceSet
	 * @return
	 */
	public String queryNeighborhood(Collection<String> sourceSet)
	{
		StringBuilder sb = new StringBuilder(endPointURL);
		sb.append(Cmd.GRAPH).append("?").append(CmdArgs.kind).append("=")
		.append(GraphType.NEIGHBORHOOD).append("&")
		.append(join(CmdArgs.source + "=", sourceSet, "&"))
		.append("&").append(CmdArgs.limit).append("=").append(graphQueryLimit);
		//the default (null) direction here would be BOTHSTREAM
		if(direction != null) 
			sb.append("&").append(CmdArgs.direction).append("=").append(direction); 
		
		return sb.toString();
	}

	
	/**
	 * Searches directed paths from and/or to the given source set of entities, in the specified search limit.
	 *
	 * @param sourceSet Set of source physical entities
	 * @param direction direction to extends network towards neighbors
	 * @return BioPAX model representing the neighborhood.
	 */
	public Model getNeighborhood(Collection<String> sourceSet)
	{
		String url = queryNeighborhood(sourceSet);
		return restTemplate.getForObject(url, Model.class);
	}

	
	/**
	 * Builds a 'COMMON STREAM' BioPAX <em>graph</em> query PROVIDER_URL string.
	 * 
	 * @see #setDirection(Direction)
	 * 
	 * @param sourceSet
	 * @return
	 */
	public String queryCommonStream(Collection<String> sourceSet)
	{		
		if (direction == Direction.BOTHSTREAM)
			throw new IllegalArgumentException(
				"Direction of common-stream query should be either upstream or downstream.");

		StringBuilder sb = new StringBuilder(endPointURL);
		sb.append(Cmd.GRAPH).append("?").append(CmdArgs.kind).append("=")
			.append(GraphType.COMMONSTREAM).append("&")
			.append(join(CmdArgs.source + "=", sourceSet, "&"))
			.append("&").append(CmdArgs.limit).append("=").append(graphQueryLimit);
		
		if(direction != null)
			sb.append("&").append(CmdArgs.direction).append("=").append(direction);
		
		return sb.toString();
	}
	
	
	/**
	 * This query searches for the common upstream (common regulators) or
	 * common downstream (common targets) objects of the given source set.
	 *
	 * @see #setDirection(Direction) (only upstream or downstream)
	 *
	 * @param sourceSet set of physical entities
	 * @return a BioPAX model that contains the common stream
	 */
	public Model getCommonStream(Collection<String> sourceSet)
	{
		String url = queryCommonStream(sourceSet);
		return restTemplate.getForObject(url, Model.class);
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
    public TraverseResponse traverse(Collection<String> uris) throws CPathException {
        String url = endPointURL + Cmd.TRAVERSE + "?" 
        		+ join(CmdArgs.uri + "=", uris, "&")
        		+ "&" + CmdArgs.path + "=" + path;

        try {
        	return restTemplate.getForObject(url, TraverseResponse.class);
		} catch (RestClientException e) {
			throw new CPathException(url, e);
		}
    }
    
    
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
	
}
