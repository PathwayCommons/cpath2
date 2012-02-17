package cpath.client;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Collections;

import org.junit.Ignore;
import org.junit.Test;

import cpath.client.util.CPathException;
import cpath.service.jaxb.SearchResponse;
import cpath.service.jaxb.ServiceResponse;
import cpath.service.jaxb.TraverseEntry;
import cpath.service.jaxb.TraverseResponse;

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
	public final void testConnectionEtc() {
		final CPath2Client client = CPath2Client.newInstance();
		Collection<String> vals = client.getValidTypes();
		assertFalse(vals.isEmpty());
		assertTrue(vals.contains("BioSource"));
		
		vals = client.getValidDataSources();
		assertFalse(vals.isEmpty());
		assertTrue(vals.contains("urn:miriam:reactome"));
		
		vals = client.getValidOrganisms();
		assertFalse(vals.isEmpty());
		assertTrue(vals.contains("urn:miriam:taxonomy:9606"));
	}
	
	
	@Test
	public final void testGetTopPathways() {
		final CPath2Client client = CPath2Client.newInstance();
		
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
		final CPath2Client cl = CPath2Client.newInstance();
		cl.setPath("Named/standardName");
		
		// must get a result w/o problems
		ServiceResponse resp = null;
        try {
			resp = cl.traverse(Collections.singleton("urn:miriam:taxonomy:9606"));
		} catch (CPathException e) {
			fail(e.toString());
		}
		assertTrue(resp instanceof TraverseResponse);
		assertFalse(((TraverseResponse)resp).getTraverseEntry().isEmpty());
		TraverseEntry entry = ((TraverseResponse)resp).getTraverseEntry().get(0);
		// check value(s)
		assertFalse(entry.getValue().isEmpty());
		assertTrue(entry.getValue().contains("Homo sapiens")); //case matters!
		
		// non-exisitng uri in not error, but the corresp. list of values must be empty
		try {
			resp = cl.traverse(Collections.singleton("bla-bla"));
		} catch (CPathException e1) {
			fail("must throw CPathException!");
		}
		assertTrue(resp instanceof TraverseResponse); //got response
		assertEquals(1, ((TraverseResponse)resp).getTraverseEntry().size()); // got entry for "bla-bla" uri
		entry = ((TraverseResponse)resp).getTraverseEntry().get(0);
		// however, - no values there!
		assertTrue(entry.getValue().isEmpty());

        
		//intentionally wrong path -> failure (error)
		cl.setPath("BioSource/participant"); 
        try {
			resp = cl.traverse(Collections.singleton("urn:miriam:taxonomy:9606"));
			fail("must throw CPathException and not something else!");
		} catch (CPathException e) {
			//ok - should be got ErrorResponse
		}
	}
	
	
	@Test
	public final void testSearch() {
		final CPath2Client cl = CPath2Client.newInstance();
        cl.setType("pathway");
		
		ServiceResponse resp = null;
        try {
			resp = cl.search("name:\"bmp signaling pathway\"");
		} catch (CPathException e) {
			fail(e.toString());
		}
		
		assertTrue(resp instanceof SearchResponse);
		assertFalse(((SearchResponse)resp).getSearchHit().isEmpty());
	}
}
