package cpath.client;

import static org.junit.Assert.*;

import java.util.Collection;

import org.junit.Ignore;
import org.junit.Test;

import cpath.service.jaxb.SearchResponse;

/**
 * INFO: when "cPath2Url" Java property is not set,
 * (e.g., -DcPath2Url="http://localhost:8080/cpath-web-service/")
 * the default cpath2 endpoint URL is {@link CPath2ClientImpl#DEFAULT_ENDPOINT_URL}
 * (e.g., http://www.pathwaycommons.org/pc2/). So, it is possible that the 
 * default (official) service still provides an older cpath2 API than this PC2 client expects.
 * Take care. 
 */
@Ignore
public class CPath2ClientTest {
	
	@Test
	public final void testConnection() {
		CPath2Client client = CPath2Client.newInstance();
		Collection<String> vals = client.getValidTypes();
		assertFalse(vals.isEmpty());
		assertTrue(vals.contains("BioSource"));
	}
	
	
	@Test
	public final void testGetTopPathways() {
		CPath2Client client = CPath2Client.newInstance();
		
		SearchResponse result = null;
		try {
			result = client.getTopPathways();
		} catch (Exception e) {
			fail(client.getEndPointURL() + " is not compartible with this test! " + e);
		}
		
		assertNotNull(result);
		assertFalse(result.getSearchHit().isEmpty());
	}
	
	@Test
	public final void testTraverse() {
		
	}
}
