
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.web.client.RestTemplate;

@Ignore
public class WebserviceTest {
	
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testGetTypes() {
		RestTemplate template = new RestTemplate();
		
		String result =
			  template.getForObject("http://localhost:8080/cpath-web-service/types", String.class);
		
		Assert.assertNotNull(result);
		
		System.out.println(result);
	}
	
}
