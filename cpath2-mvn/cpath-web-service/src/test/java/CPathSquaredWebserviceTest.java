
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
	

	@Test
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
				String.class, "http://www.biopax.org/examples/myExample#ADP"); //"HTTP://WWW.REACTOME.ORG/BIOPAX#SIGNALING_BY_BMP_1_9606");
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
	
	
	
	//@Test // not done yet
	public void testGetNeighbors() throws IOException {
		CPathWebserviceTest test = new CPathWebserviceTest();
		//test.testGetNeighbors(); // should pass unless the older web service is dead
		
		// test the new web service that is to support the old one
		test.setServiceUrl(CPATH2_SERVICE_URL+"/webservice.do");
		test.testGetNeighbors(); // aha?!
	}
	
	
	/*
	 * Legacy Command: search
	 * 
	 * webservice.do?version=2.0&q=BRCA2&output=xml&cmd=search
	 * response xml (<search_response/>) schema: 
	 * http://pathway-commons.googlecode.com/hg/cpath2-mvn/cpath-web-service/src/main/resources/schemas/cpath1-responses.xsd
	 * (previously referred as http://www.pathwaycommons.org/pc/xml/SearchResponse.xsd)
	 * 
	 * [Required] cmd=search
	 * [Required] version=2.0
	 * [Required] q= a keyword, name or external identifier.
	 * [Optional] output= xml (it's documented as 'required' but in fact - always ignored; format=xml also works...)
	 * [Optional] organism= organism filter. Must be specified as an NCBI Taxonomy identifier
	 */
	@Test
	public void testLegacySearch() {
		String result = template.getForObject(
			CPATH2_SERVICE_URL + "/webservice.do?cmd=search&version=2.0&q={q}",
			String.class, "Gly*");
		assertNotNull(result);
		// TODO check schema, check contains...
		System.out.println(result);
	}
	
	/*
	 * Command: get_pathways
	 * 
	 * http://www.pathwaycommons.org/pc/webservice.do?cmd=get_pathways&version=2.0&q=O14763&input_id_type=UNIPROT
	 * 
	 * [Required] cmd=get_pathways
	 * [Required] version=2.0
	 * [Required] q= a comma separated list of internal or external identifiers
	 * [Optional] input_id_type. Valid values:
		UNIPROT
		CPATH_ID
		ENTREZ_GENE
		GENE_SYMBOL
	 * [Optional] parameter data_source. Comma-separated values:
		BIOGRID
		CELL_MAP (not in MIRIAM yet)
		HPRD (iRefWeb)
		HUMANCYC
		IMID
		INTACT
		MINT
		NCI_NATURE
		REACTOME
	 *
	 * Note: datasource names depend on the pathwaycommons.org release;
	 *
	 * Output is a tab-delimited text with four data columns:
	 *  Database:ID, Pathway_Name, Pathway_Database_Name, and CPATH_ID
	 *  (the first line contains column names).
	 * The result row may also contain an error message, 
	 *  e.g., "XXXX    PHYSICAL_ENTITY_ID_NOT_FOUND', instead of the columns!!
	 */
	@Test
	public void testGetPathways() throws IOException {
		String result = template.getForObject(
			CPATH2_SERVICE_URL + "/webservice.do?version=2.0&cmd=get_pathways"
			+ "&input_id_type=UNIPROT&q={q}&data_source=NCI_NATURE", 
			String.class, 
			"http://www.biopax.org/examples/myExample#phosphoglucose_isomerase");
		assertTrue(result.length() > 0);
		// TODO check for pathway
		System.out.println(result);
	}
}
