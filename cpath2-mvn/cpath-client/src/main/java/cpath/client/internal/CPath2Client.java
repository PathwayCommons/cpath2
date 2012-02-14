package cpath.client.internal;

import cpath.client.internal.util.BioPAXHttpMessageConverter;
import cpath.client.internal.util.CPathExceptions;
import cpath.client.internal.util.CPathException;
import cpath.service.Cmd;
import cpath.service.CmdArgs;
import cpath.service.GraphType;
import cpath.service.jaxb.*;

import org.apache.commons.lang.StringUtils;
import org.biopax.paxtools.io.BioPAXIOHandler;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.query.algorithm.Direction;
import org.springframework.http.converter.xml.MarshallingHttpMessageConverter;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
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
	public static final String DEFAULT_ENDPOINT_URL = "http://awabi.cbio.mskcc.org/pc2/"; //finally - will be at http://www.pathwaycommons.org/pc2/
	
	private static final MarshallingHttpMessageConverter MARSHALLING_HTTP_MESSAGE_CONVERTER;
	
	private final RestTemplate restTemplate;
	
	private String endPointURL;
	private Integer page = 0;
    private Integer graphQueryLimit = 1;
    private Collection<String> organisms = new HashSet<String>();
    private Collection<String> dataSources = new HashSet<String>();
    private String type = null;
    private String path = null;

    // static initializer
    static {
        // init the JAXB marshaller/message converter once
    	Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
    	jaxb2Marshaller.setClassesToBeBound(Help.class, 
        		ServiceResponse.class, ErrorResponse.class,
        		SearchResponse.class, SearchHit.class, 
        		TraverseEntry.class, TraverseResponse.class
        );
    	MARSHALLING_HTTP_MESSAGE_CONVERTER = 
    		new MarshallingHttpMessageConverter(jaxb2Marshaller, jaxb2Marshaller);
    }
    
    
    // suppress using constructors in favor of static factories
    private CPath2Client() {
     	// create a new REST template with the xml message converter
     	restTemplate = new RestTemplate();
     	restTemplate.getMessageConverters().add(MARSHALLING_HTTP_MESSAGE_CONVERTER);
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
    	
    	// init the server URL
    	client.endPointURL = System.getProperty(JVM_PROPERTY_ENDPOINT_URL, DEFAULT_ENDPOINT_URL);
    	assert client.endPointURL != null :  "BUG: cpath2 URL is not defined!";
       
    	// add custom BioPAX http message converter
        client.restTemplate.getMessageConverters()
        	.add(new BioPAXHttpMessageConverter(bioPAXIOHandler));
        
        return client;
    }

    
    private ServiceResponse searchTemplate(Collection<String> keywords) 
    		throws CPathException 
    {
        String url = endPointURL + Cmd.SEARCH + "?" 
        	+ CmdArgs.q + "=" + join("", keywords, ",")
            + (getPage() > 0 ? "&" + CmdArgs.page + "=" + getPage() : "")
            + (getDataSources().isEmpty() ? "" : "&" + join(CmdArgs.datasource + "=", getDataSources(), "&"))
            + (getOrganisms().isEmpty() ? "" : "&" + join(CmdArgs.organism + "=", getOrganisms(), "&"))
            + (getType() != null ? "&" + CmdArgs.type + "=" + getType() : "");

        ServiceResponse resp = restTemplate.getForObject(url, ServiceResponse.class);
        if(resp instanceof ErrorResponse) {
            throw CPathExceptions.newException((ErrorResponse) resp);
        }
        return resp;
    }

    
    /**
     * Full text search. 
     * For example, retrieve a list of all records that contain "BRCA2".
     *
     * @param keyword keywords (e.g., names or identifiers); can also be a Lucene query
     * @return 
     * @throws CPathException when the WEB API gives an error
     */
    public ServiceResponse search(String keyword) throws CPathException {
        return search(Collections.singleton(keyword));
    }

    
    /**
     * Full text search. 
     *
     * @param keywords set of keywords (will be joint with 'OR'); each can be a Lucene query
     * @return
     * @throws CPathException when the WEB API gives an error
     */
    public ServiceResponse search(Collection<String> keywords) throws CPathException {
        return searchTemplate(keywords);
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
     * Retrieves details regarding one or more records, such as pathway,
     * interaction or physical entity. For example, get the complete
     * Apoptosis pathway from Reactome.
     * See http://www.pathwaycommons.org/pc2-demo/#get
     *
     * @param ids a set of BioPAX element IDs
     * @return BioPAX model containing the requested element
     */
    public Model get(Collection<String> ids) {
        String url = endPointURL + Cmd.GET + "?" 
        	+ join(CmdArgs.uri + "=" , ids, "&");
        return restTemplate.getForObject(url, Model.class);
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
		String url = endPointURL + Cmd.GRAPH + "?" + CmdArgs.kind + "=" +
			GraphType.PATHSBETWEEN.name().toLowerCase() + "&"
			+ join(CmdArgs.source + "=", sourceSet, "&") + "&"
			+ CmdArgs.limit + "=" + graphQueryLimit;

		return restTemplate.getForObject(url, Model.class);
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
		String url = endPointURL + Cmd.GRAPH + "?" + CmdArgs.kind + "=" +
			GraphType.PATHSFROMTO.name().toLowerCase() + "&"
			+ join(CmdArgs.source + "=", sourceSet, "&") + "&"
			+ join(CmdArgs.target + "=", targetSet, "&") + "&"
			+ CmdArgs.limit + "=" + graphQueryLimit;

		System.out.println(url);
		return restTemplate.getForObject(url, Model.class);
	}

	
	/**
	 * Searches directed paths from and/or to the given source set of entities, in the specified search limit.
	 *
	 * @param sourceSet Set of source physical entities
	 * @param direction direction to extends network towards neighbors
	 * @return BioPAX model representing the neighborhood.
	 */
	public Model getNeighborhood(Collection<String> sourceSet, Direction direction)
	{
		String url = endPointURL + Cmd.GRAPH + "?" + CmdArgs.kind + "=" +
			GraphType.NEIGHBORHOOD.name().toLowerCase() + "&"
			+ join(CmdArgs.source + "=", sourceSet, "&") + "&"
			+ CmdArgs.direction + "=" + direction + "&"
			+ CmdArgs.limit + "=" + graphQueryLimit;

		return restTemplate.getForObject(url, Model.class);
	}

	/**
	 * This query searches for the common upstream (common regulators) or
	 * common downstream (common targets) objects of the given source set.
	 *
	 * @param sourceSet set of physical entities
	 * @param direction upstream or downstream
	 * @return a BioPAX model that contains the common stream
	 */
	public Model getCommonStream(Collection<String> sourceSet, Direction direction)
	{
		if (direction == Direction.BOTHSTREAM)
		{
			throw new IllegalArgumentException(
				"Direction of common-stream query should be either upstream or downstream.");
		}

		String url = endPointURL + Cmd.GRAPH + "?" + CmdArgs.kind + "=" +
			GraphType.COMMONSTREAM.name().toLowerCase() + "&"
			+ join(CmdArgs.source + "=", sourceSet, "&") + "&"
			+ CmdArgs.direction + "=" + direction + "&"
			+ CmdArgs.limit + "=" + graphQueryLimit;

		return restTemplate.getForObject(url, Model.class);
	}

	
    /**
     * Gets the list of top (root) pathways 
     * (in the same xml format used by the full-text search commands)
     * 
     * @return
     */
    public SearchResponse getTopPathways() {
    	return restTemplate.getForObject(endPointURL 
    		+ Cmd.TOP_PATHWAYS, SearchResponse.class);
    }    

    
    /**
     * Gets the values (for data types) or URIs (objects) from a 
     * BioPAX property path starting from each of specified URIs.
     * 
     * @param uris
     * @return
     */
    public ServiceResponse traverse(Collection<String> uris) {
        String url = endPointURL + Cmd.GET + "?" 
        		+ join(CmdArgs.uri + "=", uris, "&")
        		+ "&" + CmdArgs.path + "=" + path;
        
        return restTemplate.getForObject(url, ServiceResponse.class);
    }
    
    
    /**
     * Can generate stings like 
     * "prefix=strings[1]&prefix=strings[2]..."
     * (if the delimiter is '&')
     * 
     * @param prefix
     * @param strings
     * @param delimiter
     * @return
     */
    private String join(String prefix, Collection<String> strings, String delimiter) {
        List<String> prefixed = new ArrayList<String>();

        for(String s: strings) {
            prefixed.add(prefix + s);
        }

        return StringUtils.join(prefixed, delimiter);
    }

    
    /**
     * The WEB Service URL prefix.
     * 
     * @return the end point URL as a string
     */
    public String getEndPointURL() {
        return endPointURL;
    }

    
    /**
     * @see #getEndPointURL()
     * @param endPointURL the end point URL as a string
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
     * @see #search(java.util.Collection)
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
    		throw new IllegalArgumentException("Negative page numbers are not supported!");
    }

    
    /**
     * Graph query search distance limit (default = 1).
     *
     * @see #getNeighborhood(Collection, Direction)
     * @see #getCommonStream(Collection, Direction)
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
     * @see #getDataSources()
     * @see #setDataSources(java.util.Collection)
     * @return valid values for the datasource parameter as a Help object.
     */
    public Help getValidDataSources() {
        return getValidParameterValues(CmdArgs.datasource.toString());
    }

    
    /**
     * @see #getOrganisms()
     * @see #setOrganisms(java.util.Collection)
     * @return valid values for the organism parameter as a Help object.
     */
    public Help getValidOrganisms() {
        return getValidParameterValues(CmdArgs.organism.toString());
    }

    
    /**
     * @see #getType()
     * @see #setType(String)
     * @return valid values for the BioPAX type parameter.
     */
    public Collection<String> getValidTypes() {
        Set<String> types = new TreeSet<String>();
    	Help help = getValidParameterValues(CmdArgs.type.toString());
    	for(Help h : help.getMembers()) {
    		types.add(h.getId());
    	}
    	return types;
    }

    private Help getValidParameterValues(String parameter) {
        String url = endPointURL + "help/" + parameter + "s";
        return restTemplate.getForObject(url, Help.class);
    }

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
    
}
