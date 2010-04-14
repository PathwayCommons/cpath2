package cpath.common.internal;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.lang.StringUtils;
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
	private static final String BASE = "http://www.uniprot.org";
	private static final Pattern PATTERN = Pattern
			.compile("^([A-N,R-Z][0-9][A-Z][A-Z, 0-9][A-Z, 0-9][0-9])|([O,P,Q][0-9][A-Z, 0-9][A-Z, 0-9][A-Z, 0-9][0-9])$");
	private static final Log log = LogFactory.getLog(UniprotClient.class);
	private static final String newline = System.getProperty("line.separator");
	private static final HttpClient client = new HttpClient();

	// some mapping capabilities (see also http://www.uniprot.org/faq/28#id_mapping_examples)
	public static final String FROM_UNIPROT = "ACC+ID";
	public static final String TO_UNIPROT = "ACC";
	public static final String REFSEQ = "P_REFSEQ_AC"; // both directions
	public static final String ENREZ_GENE = "P_ENTREZGENEID"; // both directions
	public static final String UNIGENE = "UNIGENE_ID"; // both directions
	public static final String IPI = "P_IPI"; // both directions
	public static final String ENSEMBLP = "ENSEMBL_PRO_ID"; // both directions
	public static final String ENSEMBL = "ENSEMBL_ID"; // both directions

	/**
	 * Runs a UniProt Web Tool and Returns Result.
	 * 
	 * HTTP POST Query submits the job to the queue, and it is then redirected
	 * to another location, where GET method is used to check for results.
	 * 
	 * @param tool
	 * @param params
	 * @return
	 * @throws Exception
	 */
	public static String run(String tool, NameValuePair[] params) {
		StringBuffer sb = new StringBuffer();

		String location = BASE + '/' + tool + '/';
		HttpMethod method = new PostMethod(location);
		((PostMethod) method).addParameters(params);
		method.setFollowRedirects(false);

		if (log.isInfoEnabled())
			log.info("Submitting job...");

		try {
			int status = client.executeMethod(method);
			
			// submit and get the results url
			if (status == HttpStatus.SC_MOVED_TEMPORARILY) {
				location = method.getResponseHeader("Location").getValue();
				method.releaseConnection();
				method = new GetMethod(location);
				status = client.executeMethod(method);
			}

			// wait for results are ready
			while (true) {
				int wait = 0;
				Header header = method.getResponseHeader("Retry-After");
				if (header != null)
					wait = Integer.valueOf(header.getValue());
				if (wait == 0)
					break;
				log.info("Waiting (" + wait + ")...");
				Thread.sleep(wait * 500);
				method.releaseConnection();
				method = new GetMethod(location);
				status = client.executeMethod(method);
			}

			if (status == HttpStatus.SC_OK) {
				BufferedReader in = new BufferedReader(new InputStreamReader(
						method.getResponseBodyAsStream()));
				String line = null;
				while ((line = in.readLine()) != null) {
					sb.append(line).append(newline);
				}
			} else {
				log.fatal("Failed, got " + method.getStatusLine() + " for "
						+ method.getURI());
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed", e);
		} finally {
			method.releaseConnection();
		}
		
		return sb.toString();
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
	private static String getResponceBodyAsString(GetMethod method) {
		method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
				new DefaultHttpMethodRetryHandler());
		StringBuffer sb = new StringBuffer();
		try {
			int statusCode = client.executeMethod(method);
			if (statusCode == HttpStatus.SC_OK) {
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
	
	
	public static Set<String> doMapping(String from, String to, String... ids) throws IOException {
		Set<String> toReturn = new HashSet<String>();

		NameValuePair[] nvp = new NameValuePair[] {
				new NameValuePair("from", from),
				new NameValuePair("to", to),
				new NameValuePair("format", "tab"),
				new NameValuePair("query", StringUtils.join(ids, ' ')), 
				};

		String result = UniprotClient.run("mapping", nvp);
		BufferedReader reader = new BufferedReader(new StringReader(result));
		String line = null;
		reader.readLine(); // skip title
		while((line=reader.readLine()) != null) {
			String[] cols = line.split("\\s+");
			toReturn.add(cols[1]);
		}

		return toReturn;
	}
}