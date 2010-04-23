
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.*;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Ignore
public class WebserviceTest {
	
	static RestTemplate template;
	
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
		
		String result = template.getForObject("http://localhost:8080/cpath-web-service/types", String.class);
		assertNotNull(result);
		System.out.println(result);
	}
	

	@Test
	public void testGetAllElements() {
		String result = template.getForObject("http://localhost:8080/cpath-web-service/elements/all", String.class);
		assertNotNull(result);
		System.out.println(result);
	}
	

	@Test
	public void testGetPathways() {
		String result = null;
		result = template.getForObject("http://localhost:8080/cpath-web-service/types/{type}/elements", String.class, "pathway"); 
		//note: pathway and Pathway both works (both converted to L3 Pathway class)!
		assertNotNull(result);
		assertTrue(result.contains("Pathway50"));
	}
	

	@Test
	public void testGetQueryById1() {
		String result = template.getForObject("http://localhost:8080/cpath-web-service/elements/?id={id}", 
				String.class, "http://www.biopax.org/examples/myExample#Pathway50");
		// http%3A%2F%2Fwww.biopax.org%2Fexamples%2FmyExample%23Pathway50 
		//? potential URL encoding/decoding problem here (may break NCName): - what if 'GO:12345' used instead 'Pathway50'?
		assertNotNull(result);
		System.out.println(result);
		
	}
	

	@Test
	public void testPostQueryById() {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		map.add("uri", "http://www.biopax.org/examples/myExample#Pathway50");
		String result = template.postForObject("http://localhost:8080/cpath-web-service/elements", 
				map, String.class);
		assertNotNull(result);
		System.out.println(result);
	}
	
}
