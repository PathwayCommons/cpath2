package cpath.client;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Pathway;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.junit.Ignore;
import org.junit.Test;

import cpath.client.util.CPathException;
import cpath.service.GraphType;
import cpath.service.OutputFormat;
import cpath.service.jaxb.SearchResponse;
import cpath.service.jaxb.ServiceResponse;
import cpath.service.jaxb.TraverseEntry;
import cpath.service.jaxb.TraverseResponse;

/**
 * INFO: when "cPath2Url" Java property is not set,
 * (e.g., -DcPath2Url="http://localhost:8080/cpath-web-service/")
 * the default cpath2 endpoint URL is {@link CPathClient#DEFAULT_ENDPOINT_URL}
 * So, it is possible that the default (official) service still provides 
 * an older cpath2 API than this PC2 client expects.
 * Take care. 
 */
@Ignore //these tests depend on the data, thus disabled by default (not for daily builds)
public class CPathClientTest {
	
	final static CPathClient client = CPathClient.newInstance("http://purl.org/pc2/test/"); //new stateless client	
	
	@Test
	public final void testConnectionEtc() throws CPathException {
		
		String endPointURL = client.getEndPointURL();
		System.out.println("cpath2 instance: " + endPointURL
			+ " ( is a " + client.getActualEndPointURL() + ")");
		
		//GET usually works ok with different kind of redirects...
    	String res = client.get("help", null, String.class);
    	assertTrue(res.startsWith("<?xml version="));

    	//POST
    	res = client.post("help/types", null, String.class);
//    	System.out.println(res);
    	assertTrue(res.contains("BioSource"));
	}
	
		
	@Test
	public final void testGetTopPathways() throws CPathException {		
		SearchResponse result = null;
		result = client.createTopPathwaysQuery().result();
		assertNotNull(result);
		assertFalse(result.getSearchHit().isEmpty());
		
		result = client.createTopPathwaysQuery()
			.datasourceFilter(new String[]{"reactome"}).result();		
		assertNotNull(result);
		assertFalse(result.getSearchHit().isEmpty());
		
		result = client.createTopPathwaysQuery()
			.datasourceFilter(new String[]{"foo"}).result();		
		assertNull(result);
	}

	
	@Test
	public final void testTraverse() {
		// must get a result w/o problems
    	TraverseResponse resp = null;
        try {
			resp = client.createTraverseQuery()
				.propertyPath("Named/name")
				.sources(new String[]{"http://identifiers.org/taxonomy/9606"})
				.result();			
		} catch (CPathException e) {
			fail(e.toString());
		}
		assertNotNull(resp);
		assertFalse(resp.getTraverseEntry().isEmpty());
		TraverseEntry entry = resp.getTraverseEntry().get(0);
		// check value(s)
		assertFalse(entry.getValue().isEmpty());
		assertTrue(entry.getValue().contains("Homo sapiens")); //case matters!
		
		// non-exisitng uri in not error, but the corresp. list of values must be empty
		try {
			resp = client.createTraverseQuery()
					.propertyPath("Named/name")
					.sources(new String[]{"bla-bla"})
					.result();
		} catch (CPathException e1) {
			fail("must not throw a CPathException");
		}
		assertNull(resp); //empty response
        
		//intentionally wrong path -> failure (error)
        try {
			resp = client.createTraverseQuery()
					.propertyPath("BioSource/participant")
					.sources(new String[]{"http://identifiers.org/taxonomy/9606"})
					.result();
			fail("must throw CPathException and not something else");
		} catch (CPathException e) {} //ok to ignore
        
        //test with a reactome pathway URI that contains '#' (a POST should work but GET - fail)
		try {
			resp = client.createTraverseQuery()
				.propertyPath("Pathway/pathwayComponent")
				.sources(new String[]{"http://www.reactome.org/biopax/48887#Pathway541"})
				.result();
		} catch (CPathException e) {
			fail(e.toString());
		}
        assertNotNull(resp);
        assertFalse(resp.isEmpty());
	}
	
	
	@Test
	public final void testSearch() {
		ServiceResponse resp = null;
        try {
			resp = client.createSearchQuery()
				.typeFilter(Pathway.class)
				.queryString("name:\"bmp signaling pathway\"")
				.result();
		} catch (CPathException e) {
			fail(e.toString());
		}
		
		assertTrue(resp instanceof SearchResponse);
		assertFalse(((SearchResponse)resp).getSearchHit().isEmpty());
	}
	
	
	@Test //this test is even more dependent on the data there
	public final void testGetByUri() throws CPathException {
		String id = "BRCA2"; 		
		Model m = client.createGetQuery()
			.sources(new String[]{id})
			.result();
		assertFalse(m == null);
		assertFalse(m.getObjects().isEmpty());
		assertEquals(1, m.getObjects().size()); //xref
		assertTrue(m.getObjects().iterator().next() instanceof UnificationXref);
		
		
		String res = client.createGetQuery()
				.sources(new String[]{"JUN", "PTEN"})
				.stringResult(null); //biopax
		assertNotNull(res);
		assertTrue(res.contains("biopax"));
		
		res = client.createGetQuery()
			.sources(new String[]{"JUN", "PTEN"})
				.stringResult(OutputFormat.SBGN);				
		assertNotNull(res);
//		System.out.println(res);
		assertTrue(res.contains("<sbgn"));		
	}

	
	//@Ignore
	@Test
	public final void testPathsBetweenQuery() throws CPathException
	{	
		Model model = client.createGraphQuery()
			.kind(GraphType.PATHSBETWEEN)
			.sources(new String[]{"JUN", "PTEN"})
			.limit(1)
			.result();
		
		System.out.println("model.getObjects(.size()) = " + model.getObjects().size());

		SimpleIOHandler h = new SimpleIOHandler();

		try{h.convertToOWL(model, new FileOutputStream("target/testPathsBetweenQuery.out.owl"));
		} catch (FileNotFoundException e){e.printStackTrace();}
	}
	

	@Test //this test id data-dependent
	public final void testGetPathwayByUri() throws CPathException {
		Model m = client.createGetQuery()
			.sources(new String[]{"http://www.reactome.org/biopax/48887#Pathway541"})
			.result();
        assertNotNull(m);
        assertFalse(m.getObjects(Pathway.class).isEmpty());
	}
}
