package cpath.web;

import cpath.service.api.Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;


@ActiveProfiles({"web"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebApplicationIT {

	@Autowired
	private TestRestTemplate template;

	@Autowired
	private Service service;

	@BeforeEach
	public synchronized void init() {
		if(service.getModel() == null) {
			service.init();
		}
	}

	@Test
	public void testGetTypes() {
		String result = template.getForObject("/help/types", String.class);
		assertNotNull(result);
		assertTrue(result.contains("{\"id\":\"types\",\"title\":\"BioPAX classes\""));
	}

	@Test
	public void getSearchPathway() { //API v1 allows HTTP GET
		String result = template.getForObject("/search?type={t}&q={q}", String.class, "Pathway", "Gly*");
		//note: pathway and Pathway both works (both converted to L3 Pathway class)
		assertNotNull(result);
		assertTrue(result.contains("Pathway50"));
	}

	@Test
	public void postSearchPathway() { //API v1 body contains URL-encoded query parameters
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		String body = "type=pathway&q=Gly*";
		HttpEntity<String> req = new HttpEntity<>(body, headers);
		String result = template.postForObject("/search", req, String.class);
		assertNotNull(result);
		assertTrue(result.contains("Pathway50"));
	}

	@Test
	public void getSearchV2PathwayNOK() { // API v2 does not support HTTPS GET
		String result = template.getForObject("/v2/search?type={t}&q={q}", String.class, "Pathway", "Gly*");
		assertTrue(result.contains("Method Not Allowed"));
	}

	@Test
	public void postSearchV2Pathway() { // API v2 body is a JSON object
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String body = """
    		{
    			"type": "pathway",
    			"q": "Gly*"
    		}
				""";
		HttpEntity<String> req = new HttpEntity<>(body, headers);
		String result = template.postForObject("/v2/search", req, String.class);
		assertNotNull(result);
		assertTrue(result.contains("Pathway50"));
	}

	@Test
	public void getAsSbgn()  { //API v1 HTTP GET
		String result = template.getForObject("/get?uri={uri}&format=sbgn",
				String.class, "merge:Pathway50"); //will be URL-encoded
		assertNotNull(result);
		assertTrue(result.contains("<glyph class=\"process\""));
	}

	@Test
	public void postFetchAsSbgn() { // API v2 (only HTTP POST and JSON body)
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String body = """
    		{
    			"uri": [
    				"merge:Pathway50"
    			],
    			"format": "sbgn"
    		}
				""";
		HttpEntity<String> req = new HttpEntity<>(body, headers);
		String result = template.postForObject("/v2/fetch", req, String.class);
		assertNotNull(result);
		assertTrue(result.contains("<glyph class=\"process\""));
	}

	@Test
	public void getFetchQueryNOK() { // API v2 disallows HTTP GET
		String result = template.getForObject("/v2/fetch?uri={uri}", String.class, "CALCRL");
		assertTrue(result.contains("Method Not Allowed"));
	}

	@Test
	public void postFetchQuery() {  // API v1, HTTP POST, body: URL-encoded form parameters
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		String body = "uri=bioregistry.io/uniprot:P27797"; //will be url-encoded before sent
		HttpEntity<String> req = new HttpEntity<>(body, headers);
		String result = template.postForObject("/get", req, String.class);
		assertNotNull(result);
		assertTrue(result.contains("<bp:ProteinReference rdf:about=\"bioregistry.io/uniprot:P27797\""));
	}

}
