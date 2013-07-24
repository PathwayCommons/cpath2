package cpath.query;


import org.springframework.util.MultiValueMap;

import cpath.client.CPathClient;

/**
 * A query to be executed with {@link CPathClient}
 * 
 * @author rodche
 */
abstract class BaseCPathQuery<T> {

	protected CPathClient client;	
	protected String command;
	protected String[] organism; // filter by
	protected String[] datasource; // filter by
	

	/**
	 * Builds the query parameters object.
	 * 
	 * @return
	 */
	protected abstract MultiValueMap<String, String> getRequestParams();
	
	/**
	 * @return the web service command (after the endpoint base URL before parameters)
	 */
	public String getCommand() {
		return command;
	}

	public void setClient(CPathClient client) {
		this.client = client;
	}
	
	/**
	 * Sets the filter by organism.
	 * @param organisms a set of organism names/taxonomy or null (no filter)
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public CPathQuery<T> organismFilter(String[] organisms) {
		this.organism = organisms;
		return (CPathQuery<T>) this;
	}
	
	/**
	 * Sets the filter by pathway data source.
	 * @param datasources a set of data source names/URIs, or null (no filter)
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public CPathQuery<T> datasourceFilter(String[] datasources) {
		this.datasource = datasources;
		return (CPathQuery<T>) this;
	}
}
