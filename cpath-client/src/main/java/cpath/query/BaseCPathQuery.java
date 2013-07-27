package cpath.query;


import java.util.Collection;

import org.apache.commons.lang.ArrayUtils;
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

}
