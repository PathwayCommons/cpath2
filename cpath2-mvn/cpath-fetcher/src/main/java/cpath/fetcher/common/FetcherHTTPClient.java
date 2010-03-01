package cpath.fetcher.common;

// imports
import java.io.IOException;
import java.util.Collection;

public interface FetcherHTTPClient {
	
	/**
	 * Fetches data from given URL and passes on to reader for processing.
	 * 
	 * @param url String
	 * @param serviceReader ServiceReader
     * @param Collection<T>
	 * @throws IOException if an IO error occurs
	 */
	<T> void getDataFromService(final String url, final ServiceReader serviceReader, final Collection<T> toReturn) throws IOException;

	/**
	 * Fetches data from given URL and returns as byte[].
	 * 
	 * @param url String
     * @return byte[]
	 * @throws IOException if an IO error occurs
	 */
	byte[] getDataFromService(final String url) throws IOException;
}