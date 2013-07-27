package cpath.client;

import cpath.client.util.BioPAXHttpMessageConverter;
import cpath.client.util.CPathException;
import cpath.client.util.ServiceResponseHttpMessageConverter;
import cpath.query.CPathGetQuery;
import cpath.query.CPathGraphQuery;
import cpath.query.CPathSearchQuery;
import cpath.query.CPathTopPathwaysQuery;
import cpath.query.CPathTraverseQuery;
import cpath.service.jaxb.*;

import org.apache.commons.lang.StringUtils;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.UnknownHttpStatusCodeException;

import java.util.*;
import java.util.Map.Entry;


/**
 * New stateless cPath2 client with create* 
 * methods to conveniently build and run queries.
 */
public class CPathClient
{
	private static final Logger LOGGER = LoggerFactory.getLogger(CPathClient.class);
	
	// one can set the JVM property: -DcPath2Url="http://some_URL"
	private static final String JVM_PROPERTY_ENDPOINT_URL = "cPath2Url";	
	private static final String DEFAULT_ENDPOINT_URL = "http://www.pathwaycommons.org/pc2/";
		
	private final RestTemplate restTemplate;
	
	private String endPointURL;
	private String actualEndPointURL;

	public static enum Direction
    {
		UPSTREAM, DOWNSTREAM, BOTHSTREAM;
    }
	
	// suppress using constructors in favor of static factories
    private CPathClient() {
     	// create a new REST template
     	restTemplate = new RestTemplate(); //custom message converters will be added there
    }
    
    
    /**
     * Instantiates the client using the default endpoint URL.
     */
    public static CPathClient newInstance() {
    	return newInstance(null);
    }
    
    
    /**
     * Instantiates the client using the cpath2 endpoint URL.
     * 
     * @param url cpath2 web service endpoint URL or null (to use defaults)
     */
    public static CPathClient newInstance(String url) {
    	CPathClient client = new CPathClient(); 
    	
     	// add custom cPath2 XML message converter as the first one (accepts 'application/xml' content type)
    	// because one of existing/default msg converters, XML root element based jaxb2, does not work for ServiceResponce types...
    	client.restTemplate.getMessageConverters().add(0, new ServiceResponseHttpMessageConverter());
    	// add BioPAX http message converter
        client.restTemplate.getMessageConverters().add(1, new BioPAXHttpMessageConverter(new SimpleIOHandler()));
    	
        // set the cpath2 server URL (or default one or from the java option)
    	if(url == null || url.isEmpty())
    		url = System.getProperty(JVM_PROPERTY_ENDPOINT_URL, DEFAULT_ENDPOINT_URL);  	
    	
    	client.setEndPointURL(url);
    	
    	assert client.actualEndPointURL != null :  "cPath2 endpoint URL is undefined";
    	
    	return client;
    }
    
	
	/**
	 * Sends a HTTP POST (preferred, more reliable with complex queries) 
	 * request to the server.
	 * 
	 * @param requestPath cpath2 web service command path (e.g., search, help/types, etc.)
	 * @param requestParams query parameters object (e.g. {@link MultiValueMap} or a java bean).
	 * @param responseType result class (e.g., String.class, Model)
	 * @return
	 * @throws CPathException
	 */
	public <T> T post(String requestPath, Object requestParams, Class<T> responseType)
		throws CPathException 
	{
		final String url = actualEndPointURL + requestPath;
		
		try {
			return restTemplate.postForObject(url, requestParams, responseType);
		} catch (UnknownHttpStatusCodeException e) {
			if (e.getRawStatusCode() == 460) {
				return null; //empty result
			} else
				throw new CPathException(url + " and " + requestParams.toString(), e);
		} catch (RestClientException e) {
			throw new CPathException(url + " and " + requestParams.toString(), e);
		}
	}
    
	
	/**
	 * Sends a HTTP GET request to the cpath2 server.
	 * 
	 * Note: using {@link #post(String, Object, Class)} is the preferred 
	 * and more reliable method, especially with complex queries that use URIs or
	 * Lucene syntax.
	 * 
	 * @param requestPath cpath2 web service command path (e.g., search, help/types, etc.)
	 * @param requestParams query parameters map
	 * @param responseType result class (e.g., String.class, Model)
	 * @return
	 * @throws CPathException
	 */
	public <T> T get(String requestPath, MultiValueMap<String, String> requestParams, Class<T> responseType)
		throws CPathException 
	{	
		StringBuilder sb = new StringBuilder(actualEndPointURL);
		sb.append(requestPath);
		
		if(requestParams != null) {
			sb.append("?");
			for (Entry<String, List<String>> entry : requestParams.entrySet()) {
				String params = join(entry.getKey() + "=", entry.getValue(), "&");
				sb.append(params).append("&");
			}
		}
		
		String url = sb.toString();
		
		try {
			return restTemplate.getForObject(url, responseType);
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
     * Retrieves information about available cPath2 commands and their parameters.
     * 
     * @param hpath relative (ie., as in 'help/[path]') REST query path variable; e.g.: null (all), "datasources", "commands/search", etc.
     * 
     * @return
     * @throws CPathException 
     */
    public Help executeHelp(String hpath) throws CPathException {
    	return get("help/" + ((hpath != null) ? hpath : ""), null, Help.class);
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
     * cPath2 Web Service URL.
     * 
     * @return the cpath2 end point URL as a string
     */
    public String getEndPointURL() {
        return endPointURL;
    }
    
    
    /**
     * Actual cPath2 Web Service URL that is 
     * resolved from the {@link #endPointURL} by
     * following HTTP (302, 301) redirects.
     * 
     * @return the resolved cpath2 end point URL
     */
    public String getActualEndPointURL() {
        return actualEndPointURL;
    }

    
    /**
     * @see #getEndPointURL()
     * @param endPointURL the cpath2 end point URL as string
     */
    public void setEndPointURL(String endPointURL) {
        this.endPointURL = endPointURL;
        
        if(this.endPointURL.equals(this.actualEndPointURL))
        	return; // nothing else to do.
    	
    	// discover the actual URL (the first one that works via the POST method)
    	actualEndPointURL = this.endPointURL; //initial value
    	while(true) {
    		ResponseEntity<String> re = restTemplate
    			.exchange(actualEndPointURL, HttpMethod.HEAD, null, String.class);  		
    		
    		if(re.getStatusCode().equals(HttpStatus.FOUND)
    			|| re.getStatusCode().equals(HttpStatus.MOVED_PERMANENTLY)) {
    			actualEndPointURL = re.getHeaders().getLocation().toString();
    			LOGGER.info("Found new location: " + actualEndPointURL 
        				+ "; " + re.getStatusCode());
    		}
    		else if(re.getStatusCode().equals(HttpStatus.OK)) {
    			LOGGER.info("OK: will be using" + actualEndPointURL 
    				+ " instead of " + endPointURL);
    			break; //exit the infinite loop
    		}
    		else { //for all other status codes
    			throw new RuntimeException("HTTP POST failed " +
    				"after the client was redirected " +
    				"from " + endPointURL +	" to " + actualEndPointURL 
    				+ " (status: " + re.getStatusCode() + ")");
    		}
    	}
    }
	
	
	/**
	 * Creates a new full-text search query object
	 * (e.g., call as cli.createSearchQuery().queryString("BRCA*")
	 * .typeFilter(Pathway.class).dataSourceFilter("reactome").result();)
	 * 
	 * @return
	 */
	public CPathSearchQuery createSearchQuery() {
		return new CPathSearchQuery(this);
	}
	
	/**
	 * Creates a new biopax graph properties traverse query 
	 * (e.g., call as cli.createTraverseQuery().source(..).propertyPath(..).result();)
	 * 
	 * @return
	 */
	public CPathTraverseQuery createTraverseQuery() {
		return new CPathTraverseQuery(this);
	}
	
	/**
	 * Creates a new advanced biopax graph query 
	 * to calculate and fetch a biopax sub-model from the web service
	 * (e.g., call as cli.createGraphQuery().kind(k).limit(n).source(srcs).result();)
	 * 
	 * @return
	 */
	public CPathGraphQuery createGraphQuery() {
		return new CPathGraphQuery(this);
	}
	
	/**
	 * Creates a new get-by-id (or by URI) query to 
	 * fetch a biopax sub-model from the web service
	 * (e.g., call as model = cli.createGetQuery().ids(..).result();)
	 * 
	 * @return
	 */
	public CPathGetQuery createGetQuery() {
		return new CPathGetQuery(this);
	}
		
	/**
	 * Creates a new "top pathways" query object
	 * (e.g., call as cli.createTopPathwaysQuery()
	 * 					 .dataSourceFilter("reactome")
	 * 					 .result();
	 * )
	 * 
	 * @return
	 */
	public CPathTopPathwaysQuery createTopPathwaysQuery() {
		return new CPathTopPathwaysQuery(this);
	}
}
