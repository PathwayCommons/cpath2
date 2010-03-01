package cpath.fetcher.common.internal;

// imports
import cpath.fetcher.common.ServiceReader;
import cpath.fetcher.common.FetcherHTTPClient;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collection;

/**
 * FetcherHTTPClient implementation.
 */
@Service
public final class FetcherHTTPClientImpl implements FetcherHTTPClient {

    /**
     * (non-Javadoc)
     * @see cpath.fetcher.common.FetcherHTTPClient#getDataFromService(java.lang.String, cpath.fetcher.common.ServiceReader)
     */
    @Override
	public <T> void getDataFromService(final String url, final ServiceReader serviceReader, final Collection<T> toReturn) throws IOException {

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
                serviceReader.readFromService(method.getResponseBodyAsStream(), toReturn);
            }
        }
        finally {
            method.releaseConnection();
        }
    }

    /**
     * (non-Javadoc)
     * @see cpath.fetcher.common.FetcherHTTPClient#getDataFromService(java.lang.String)
     */
	@Override
	public byte[] getDataFromService(final String url) throws IOException {

        byte[] toReturn = null;

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
                toReturn =  method.getResponseBody();
            }
        }
        finally {
            method.releaseConnection();
        }

        // outta here
        return toReturn;
    }
}
