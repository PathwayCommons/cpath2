package cpath.fetcher.internal;

// imports
import cpath.fetcher.FetcherHTTPClient;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

/**
 * FetcherHTTPClient implementation.
 */
@Service
public final class FetcherHTTPClientImpl implements FetcherHTTPClient {

	// made a property to be closed
	private HttpMethod method;

    /**
     * (non-Javadoc)
     * @see cpath.fetcher.FetcherHTTPClient#getDataFromService(java.lang.String)
     */
    @Override
	public byte[] getDataFromService(final String url) throws IOException {
		
        // setup httpclient and method
        HttpClient httpClient = new HttpClient();
        GetMethod method = new GetMethod(url);
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, 
                                        new DefaultHttpMethodRetryHandler());
        try {
            // execute
            int statusCode = httpClient.executeMethod(method);

            // get the output
            if (statusCode == 200) {
				return method.getResponseBody();
            }
        }
        finally {
            method.releaseConnection();
        }

		// should not get here
		return null;
    }

    /**
     * (non-Javadoc)
     * @see cpath.fetcher.FetcherHTTPClient#getDataFromService(java.lang.String)
     */
    @Override
	public InputStream getDataFromServiceAsStream(final String url) throws IOException {

		InputStream toReturn = null;

        // setup httpclient and method
        HttpClient httpClient = new HttpClient();
        method = new GetMethod(url);
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, 
                                        new DefaultHttpMethodRetryHandler());

		// execute
		int statusCode = httpClient.executeMethod(method);

		// get the output
		if (statusCode == 200) {
			toReturn = method.getResponseBodyAsStream();
		}

		// outta here
		return toReturn;
    }

    /**
     * (non-Javadoc)
     * @see cpath.fetcher.FetcherHTTPClient#releaseConnection
     */
    @Override
	public void releaseConnection() {

		if (method != null) {
			method.releaseConnection();
		}
	}
	
    
    @Override
	public InputStream getDataFromService(final String url, NameValuePair[] params) 
    	throws IOException {

        // setup httpclient and method
        HttpClient httpClient = new HttpClient();
        method = new PostMethod(url);
        ((PostMethod)method).addParameters(params);
        //method.setFollowRedirects(false);
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, 
                                        new DefaultHttpMethodRetryHandler());

		// execute
		int statusCode = httpClient.executeMethod(method);

		// get the output
		if (statusCode == 200) {
			return method.getResponseBodyAsStream();
		}

		// outta here
		return null;
    }
}
