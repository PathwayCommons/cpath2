package cpath.common.internal;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * 
 * Example: 
 * <pre>
 *   public static void main(String[] args) {
 *   String data = UniprotClient.run("mapping", new NameValuePair[] {
 *     new NameValuePair("from", "ACC"),
 *     new NameValuePair("to", "P_REFSEQ_AC"),
 *     new NameValuePair("format", "tab"),
 *     new NameValuePair("query", "P13368 P20806 Q9UM73 P97793 Q17192"),
 *   });
 *   
 * }
 *</pre>
 * 
 * @author rodche
 *
 */
public class UniprotClient {
	public static final String BASE = "http://www.uniprot.org";
	public static final Pattern PATTERN = Pattern
			.compile("^([A-N,R-Z][0-9][A-Z][A-Z, 0-9][A-Z, 0-9][0-9])|([O,P,Q][0-9][A-Z, 0-9][A-Z, 0-9][A-Z, 0-9][0-9])$");
	private static final Log log = LogFactory.getLog(UniprotClient.class);
	private static final String newline = System.getProperty("line.separator");
	private static final HttpClient client = new HttpClient();

	
	/**
	 * Runs a UniProt Web Tool and Returns Result (HTTP POST Query). 
	 * 
	 * @param tool
	 * @param params
	 * @return
	 */
	public static String run(String tool, NameValuePair[] params) {
		String location = BASE + '/' + tool + '/';
		PostMethod method = new PostMethod(location);
		method.addParameters(params);
		method.setFollowRedirects(false);
		return getResponceBodyAsString(method);
	}

	/**
	 * Get UniProt Primary Accession by a Secondary One.
	 * 
	 * @param id
	 * @return
	 */
	public static String getPrimaryId(String id) {
		String query = BASE	+ "/uniprot?format=tab&columns=id&query=accession:" + id;
		try {
			String data = getResponceBodyAsString(new GetMethod(query));
			BufferedReader in = new BufferedReader(new StringReader(data));
			// primary ID always appears first in the returned table
			String line = in.readLine(); // skip title
			line = in.readLine();
			Matcher matcher = PATTERN.matcher(line.trim());
			if (matcher.find()) {
				return matcher.group();
			} else {
				return null;
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed query " + query, e);
		}
	}

	/**
	 * Gets Taxnonomy Id by UniProt Accession.
	 * 
	 * @param id
	 * @return
	 */
	public static Integer getTaxon(String id) {
		String query = BASE
				+ "/uniprot?format=tab&columns=taxon&query=accession:" + id;
		try {
			String data = getResponceBodyAsString(new GetMethod(query));
			BufferedReader in = new BufferedReader(new StringReader(data));
			String line = in.readLine(); // skip title line
			line = in.readLine();
			return Integer.parseInt(line.trim());
		} catch (Exception e) {
			throw new RuntimeException("Failed query " + query, e);
		}
	}

	
	/*
	 * Handles HTTP request/response retries and errors
	 */
	private static String getResponceBodyAsString(HttpMethod method) {
		method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
				new DefaultHttpMethodRetryHandler());
		StringBuffer sb = new StringBuffer();
		try {
			int statusCode = client.executeMethod(method);
			if (statusCode == 200) {
				BufferedReader in = new BufferedReader(
						new InputStreamReader(method.getResponseBodyAsStream()));
				String line;
				while((line=in.readLine()) != null) {
					sb.append(line).append(newline);
				}
			}
		} catch (HttpException e) {
			throw new RuntimeException("Failed", e);
		} catch (IOException e) {
			throw new RuntimeException("Failed", e);
		} finally {
			method.releaseConnection();
		}
		
		return sb.toString();
	}
}