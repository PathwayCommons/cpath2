
import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;


/**
 * Requires system property "cPath2Url", e.g, 
 * -DcPath2Url="http://localhost:8080/"
 * JVM property
 */
@Ignore // TODO comment out @Ignore and run tests when the WS is up and running
public class CPathSquaredWebserviceTest {
	
	static RestTemplate template;

	//default test endpoint
	static String serviceUrl = "http://localhost:8080/";
	
	static {
		template = new RestTemplate();
		List<HttpMessageConverter<?>> msgCovs = new ArrayList<HttpMessageConverter<?>>();
		msgCovs.add(new FormHttpMessageConverter());
		msgCovs.add(new StringHttpMessageConverter());
		template.setMessageConverters(msgCovs);
		
		String url = System.getProperty("cPath2Url");
		if(url != null && !url.isEmpty())
			serviceUrl = url;
		
	}
	
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testGetTypes() {
		String result = template.getForObject(serviceUrl+"/help/types", String.class);
		assertNotNull(result);
		System.out.println(result);
	}
	

	@Test
	public void testSearchPathway() {
		String result = null;
		result = template.getForObject(serviceUrl + 
				"/search?type={t}&q={q}", String.class, "Pathway", "Gly*"); 
		//note: pathway and Pathway both works (both converted to L3 Pathway class)
		assertNotNull(result);
		assertTrue(result.contains("Pathway50"));
	}
	

	//HTTP GET
	@Test
	public void testGetQueryById() {
		String result = template.getForObject(serviceUrl+"/get?uri={uri}", 
				String.class, 
				"http://identifiers.org/uniprot/P27797");
		assertNotNull(result);
	}
	
	
	//HTTP POST
	@Test
	public void testPostQueryById() {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		map.add("uri", "http://identifiers.org/uniprot/P27797");
		String result = template.postForObject(serviceUrl+"/get", 
				map, String.class);
		assertNotNull(result);
	}

}
