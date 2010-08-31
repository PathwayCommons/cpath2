
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.*;

import org.junit.*;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Ignore // TODO comment out @Ignore and run tests when the WS is up and running
public class CPathSquaredWebserviceTest {
	
	static RestTemplate template;
	//static final String CPATH2_SERVICE_URL = "http://www.pathwaycommons.org/pc/webservice";
	static final String CPATH2_SERVICE_URL = "http://localhost:8080/cpath-web-service"; // Temp.	
	
	static {
		template = new RestTemplate();
		List<HttpMessageConverter<?>> msgCovs = new ArrayList<HttpMessageConverter<?>>();
		msgCovs.add(new FormHttpMessageConverter());
		msgCovs.add(new StringHttpMessageConverter());
		template.setMessageConverters(msgCovs);
	}
	
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testGetTypes() {
		String result = template.getForObject(CPATH2_SERVICE_URL+"/help/types", String.class);
		assertNotNull(result);
		System.out.println(result);
	}
	

	@Test
	public void testGetPathways() {
		String result = null;
		result = template.getForObject(CPATH2_SERVICE_URL + 
				"/search?type={t}&q={q}", String.class, "Pathway", "Gly*"); 
		//note: pathway and Pathway both works (both converted to L3 Pathway class)!
		assertNotNull(result);
		assertTrue(result.contains("Pathway50"));
	}
	

	//HTTP GET
	@Test
	public void testGetQueryById1() {
		String result = template.getForObject(CPATH2_SERVICE_URL+"/get?uri={uri}", 
				String.class, "http://www.biopax.org/examples/myExample#ADP");
		//any URL encoding/decoding problem here? What if 'GO:12345' used instead 'Pathway50'?
		assertNotNull(result);
		System.out.println(result);
	}
	
	
	//HTTP POST
	@Test
	public void testPostQueryById() {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		map.add("uri", "http://www.biopax.org/examples/myExample#Pathway50");
		String result = template.postForObject(CPATH2_SERVICE_URL+"/get", 
				map, String.class);
		assertNotNull(result);
	}
	
	
	
	@Test // not done yet
	public void testGetNeighbors() throws IOException {
		CPathWebserviceTest test = new CPathWebserviceTest();
		//test.testGetNeighbors(); // should pass unless the older web service is dead
		
		// test the new web service that is to support the old one
		test.setServiceUrl(CPATH2_SERVICE_URL+"/webservice.do");
		test.testGetNeighbors(); // aha?!
	}
	
}
