package cpath.query;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import org.apache.commons.lang.ArrayUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import cpath.client.CPathClient;
import cpath.client.util.CPathException;
import cpath.service.Cmd;
import cpath.service.CmdArgs;
import cpath.service.OutputFormat;
import cpath.service.jaxb.SearchHit;
import cpath.service.jaxb.SearchResponse;

/**
 * A "get top pathways" query to be executed with {@link CPathClient}
 * on the cpath2 server's biopax database.
 * 
 * @author rodche
 */
public final class CPathTopPathwaysQuery extends BaseCPathQuery<SearchResponse> implements CPathQuery<SearchResponse> {

	protected MultiValueMap<String, String> getRequestParams() {
		MultiValueMap<String, String> request = new LinkedMultiValueMap<String, String>();
		if(organism != null)
			request.put(CmdArgs.organism.name(), Arrays.asList(organism));
		if(datasource != null)
			request.put(CmdArgs.datasource.name(), Arrays.asList(datasource));
		return request;
	}

	/**
	 * Constructor.
	 * 
	 * @param client instance of the cpath2 client
	 */
	public CPathTopPathwaysQuery(CPathClient client) {
		this.client = client;
		this.command = Cmd.TOP_PATHWAYS.toString();
	}

	/**
	 * Sets the filter by organism.
	 * @param organisms a set of organism names/taxonomy or null (no filter)
	 * @return
	 */
	public CPathTopPathwaysQuery organismFilter(String[] organisms) {
		this.organism = organisms;
		return this;
	}
	
	/**
	 * Sets the filter by pathway data source.
	 * @param datasources a set of data source names/URIs, or null (no filter)
	 * @return
	 */
	public CPathTopPathwaysQuery datasourceFilter(String[] datasources) {
		this.datasource = datasources;
		return this;
	}	
	
	/**
	 * Sets the filter by organism.
	 * @param organisms a set of organism names/taxonomy or null (no filter)
	 * @return
	 */
	public CPathTopPathwaysQuery organismFilter(Collection<String> organisms) {
		this.organism = organisms.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
		return this;
	}
	
	/**
	 * Sets the filter by pathway data source.
	 * @param datasources a set of data source names/URIs, or null (no filter)
	 * @return
	 */
	public CPathTopPathwaysQuery datasourceFilter(Collection<String> datasources) {
		this.datasource = datasources.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
		return this;
	}	
	
	@Override
	public String stringResult(OutputFormat outputFormat) throws CPathException {
		throw new UnsupportedOperationException();
	}
	

	@Override
	public SearchResponse result() throws CPathException {
		SearchResponse resp = client.post(command, getRequestParams(), SearchResponse.class);
		
    	Collections.sort(resp.getSearchHit(), new Comparator<SearchHit>() {
			@Override
			public int compare(SearchHit h1, SearchHit h2) {
				return h1.toString().compareTo(h2.toString());
			}
		}); 
    	
    	return resp;
	}
	
}
