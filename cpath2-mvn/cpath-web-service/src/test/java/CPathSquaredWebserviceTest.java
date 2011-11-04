
import static org.junit.Assert.*;

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
	//static final String CPATH2_SERVICE_URL = "http://www.pathwaycommons.org/pc2";
	//static final String CPATH2_SERVICE_URL = "http://awabi.cbio.mskcc.org/cpath2"; // development instance
	static final String CPATH2_SERVICE_URL = "http://localhost:8080/cpath-web-service";	
	
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
	

	//@Test
	public void testSearchPathway() {
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
				String.class, 
				//"http://www.biopax.org/examples/myExample#ADP"); 
				"HTTP://WWW.REACTOME.ORG/BIOPAX/48887#PATHWAY1076_1_9606");
		assertNotNull(result);
		System.out.println(result);
	}
	
	
	//HTTP POST
	//@Test
	public void testPostQueryById() {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		map.add("uri", "http://www.biopax.org/examples/myExample#Pathway50");
		String result = template.postForObject(CPATH2_SERVICE_URL+"/get", 
				map, String.class);
		assertNotNull(result);
	}

}
