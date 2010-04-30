
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.junit.*;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@Ignore
public class CPathWebserviceTest {
	
	static final RestTemplate template;
	static final HttpClient client;
	static final String CPATH_SERVICE_URL = "http://www.pathwaycommons.org/pc/webservice.do";
	
	static {
		template = new RestTemplate();
		List<HttpMessageConverter<?>> msgCovs = new ArrayList<HttpMessageConverter<?>>();
		msgCovs.add(new FormHttpMessageConverter());
		msgCovs.add(new StringHttpMessageConverter());
		template.setMessageConverters(msgCovs);
		client = new HttpClient();
	}

	@Test
	public void testSearchWithRestTemplate() {
		String result = template.getForObject(
				CPATH_SERVICE_URL + "?version=2.0&q=BRCA2&format=xml&cmd=search"
				, String.class);
		assertNotNull(result);
		System.out.println(result);
	}
	
	@Test
	public void testSearchWithUrl() throws IOException {
		URL url = new URL(CPATH_SERVICE_URL + 
				"?version=2.0&q=BRCA2&format=xml&cmd=search");
		BufferedReader in = new BufferedReader(
				new InputStreamReader(url.openStream()));
		StringBuffer result = new StringBuffer();
		String line;
		while((line = in.readLine()) != null) {
			result.append(line)
				.append(System.getProperty("line.separator"));
		}
		in.close();
		assertTrue(result.toString().length()>0);
		System.out.println(result.toString());
	}
	
	@Test
	public void testSearchWithHttpClient() throws HttpException, IOException {
		GetMethod method = new GetMethod(CPATH_SERVICE_URL + 
				"?version=2.0&q=BRCA2&format=xml&cmd=search");
		method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
				new DefaultHttpMethodRetryHandler());
		StringBuffer sb = new StringBuffer();
		int statusCode = client.executeMethod(method);
		if (statusCode == HttpStatus.SC_OK) {
			BufferedReader in = new BufferedReader(
				new InputStreamReader(method.getResponseBodyAsStream()));
			String line;
			while((line=in.readLine()) != null) {
				sb.append(line)
					.append(System.getProperty("line.separator"));
			}
		}
		method.releaseConnection();
		String result = sb.toString();
		assertTrue(result.length()>0);
	}
	
}
