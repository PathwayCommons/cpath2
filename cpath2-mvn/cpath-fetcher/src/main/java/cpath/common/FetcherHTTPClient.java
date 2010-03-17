package cpath.common;

// imports
import java.io.IOException;
import java.io.InputStream;

public interface FetcherHTTPClient {

	/**
	 * Fetches data from given URL.
	 * 
	 * @param url String
     * @return byte[]
	 * @throws IOException if an IO error occurs
	 */
	byte[] getDataFromService(final String url) throws IOException;
	
	/**
	 * Fetches data from given URL.
	 * 
	 * @param url String
     * @return InputStream
	 * @throws IOException if an IO error occurs
	 */
	InputStream getDataFromServiceAsStream(final String url) throws IOException;

	/**
	 * Releases connection.
	 * Required after a call to getDataFromServiceAsStream is made and processed.
	 */
	void releaseConnection();
}