import cpath.Application;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.UnsupportedEncodingException;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@ActiveProfiles({"web"})
@SpringBootTest(classes = {Application.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebAppIT {

  @Autowired
  private TestRestTemplate template;

	@Test
	public void testGetTypes() {
		String result = template.getForObject("/help/types", String.class);
		assertNotNull(result);
		assertTrue(result.contains("{\"id\":\"types\",\"title\":\"BioPAX classes\""));
	}


	@Test
	public void testSearchPathway() {
		String result;
		result = template.getForObject("/search?type={t}&q={q}", String.class, "Pathway", "Gly*");
		//note: pathway and Pathway both works (both converted to L3 Pathway class)
		assertNotNull(result);
		assertTrue(result.contains("Pathway50"));
	}

	//HTTP GET
	@Test
	public void testGetQueryById() throws UnsupportedEncodingException {
		String result = template.getForObject("/get?uri={uri}",
				String.class, "http://identifiers.org/uniprot/P27797");
    assertNotNull(result);
		assertTrue(result.contains("<bp:ProteinReference rdf:about=\"http://identifiers.org/uniprot/P27797\""));
	}


	@Test //if POST isn't disabled
	public void testPostQueryById() {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("uri", "http://identifiers.org/uniprot/P27797");
		String result = template.postForObject("/get", map, String.class);
    assertNotNull(result);
    assertTrue(result.contains("<bp:ProteinReference rdf:about=\"http://identifiers.org/uniprot/P27797\""));
//		assertTrue(result.contains("Method Not Allowed"));
	}

}
