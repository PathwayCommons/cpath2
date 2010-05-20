
import static org.junit.Assert.*;

import java.io.*;
import java.net.URL;
import java.util.*;

import javax.xml.bind.*;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.junit.*;
import org.springframework.http.converter.*;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import cpath.webservice.jaxb.ErrorType;
import cpath.webservice.jaxb.ExtendedRecordType;
import cpath.webservice.jaxb.SearchResponseType;


//@Ignore
public class CPathWebserviceTest {
	static final RestTemplate template;
	static final HttpClient client;
	static final String CPATH_SERVICE_URL = "http://www.pathwaycommons.org/pc/webservice.do";
	
	static {
		// init RestTemplate
		template = new RestTemplate();
		List<HttpMessageConverter<?>> msgCovs = new ArrayList<HttpMessageConverter<?>>();
		msgCovs.add(new FormHttpMessageConverter());
		msgCovs.add(new StringHttpMessageConverter());
		msgCovs.add(new SourceHttpMessageConverter<Source>());
		template.setMessageConverters(msgCovs);
		// commons-httpclient
		client = new HttpClient();
	}

	/*
	 * Command: search
	 * 
	 * http://www.pathwaycommons.org/pc/webservice.do?version=2.0&q=BRCA2&output=xml&cmd=search
	 * response xml (<search_response/>) schema: 
	 * http://pathway-commons.googlecode.com/hg/cpath2-mvn/cpath-web-service/src/main/resources/schemas/cpath1-responses.xsd
	 * (previously referred as http://www.pathwaycommons.org/pc/xml/SearchResponse.xsd)
	 * 
	 * [Required] cmd=search
	 * [Required] version=2.0
	 * [Required] q= a keyword, name or external identifier.
	 * [Required] output= xml (in fact, it is 'xml' by default, and format=xml also works...)
	 * [Optional] organism= organism filter. Must be specified as an NCBI Taxonomy identifier
	 */
	@Test
	public void testSearchWithRestTemplate() throws JAXBException {
		String result = template.getForObject(CPATH_SERVICE_URL + 
				"?version=2.0&q={q}&cmd=search", String.class, "BRCA2");
		assertNotNull(result);
		// Wow, we got some xml! Let's unmarshal it to do something...
		JAXBContext jaxbContext = JAXBContext.newInstance("cpath.webservice.jaxb");
		Unmarshaller un = jaxbContext.createUnmarshaller();
		JAXBElement<?> element = (JAXBElement<?>) un.unmarshal(
				new ByteArrayInputStream(result.getBytes()));
		Object response = element.getValue();
		if(response instanceof ErrorType) {
			System.out.println(((ErrorType)response).getErrorMsg());
		} else {
			SearchResponseType searchResponseType = un.unmarshal(
					new StreamSource(new StringReader(result)), 
					SearchResponseType.class).getValue();
			assertTrue(searchResponseType.getTotalNumHits()>0);
			// print the first  hit only -
			ExtendedRecordType rec = searchResponseType.getSearchHit().get(0);
			Marshaller ma = jaxbContext.createMarshaller();
			ma.setProperty("jaxb.formatted.output", true);
			ma.marshal( new JAXBElement(new QName("","search_hit"), 
					  rec.getClass(), rec ), System.out);
		}
	}

	/*
	 * Another variant that gets the SearchResponseType! 
	 */
	@Test
	public void testSearchWithRestTemplate2() throws JAXBException {
		/* MarshallingHttpMessageConverter did not work...
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		//marshaller.setSchema(new ClassPathResource("schemas/cpath1-responses.xsd"));
		marshaller.setContextPath("cpath.webservice.jaxb");
		MarshallingHttpMessageConverter messageConverter = 	new MarshallingHttpMessageConverter(marshaller);
		messageConverter.setUnmarshaller(marshaller);
		template.getMessageConverters().add(messageConverter.setUnmarshaller);
		*/
		Source source = template.getForObject(CPATH_SERVICE_URL + 
				"?version=2.0&q=BRCA2&output=xml&cmd=search", Source.class);
		assertNotNull(source);
		JAXBContext jaxbContext = JAXBContext.newInstance("cpath.webservice.jaxb");
		// unlike above tests, we do not check for ErrorType here
		SearchResponseType result =jaxbContext.createUnmarshaller()
			.unmarshal(source, SearchResponseType.class).getValue();
		assertNotNull(result);
		assertTrue(result.getTotalNumHits()>0);
	}

	
	/*
	 * Probably, the simplest and often the best method...
	 */
	@Test
	public void testSearchWithUrl() throws IOException {
		URL url = new URL(CPATH_SERVICE_URL + 
				"?version=2.0&q=BRCA2&output=xml&cmd=search");
		BufferedReader in = new BufferedReader(
				new InputStreamReader(url.openStream()));
		StringBuffer result = new StringBuffer();
		String line;
		while((line = in.readLine()) != null) {
			result.append(line)
				.append(System.getProperty("line.separator"));
		}
		in.close();
		assertTrue(result.toString().length()>0);
		//System.out.println(result);
	}

	
	@Test
	public void testSearchWithHttpClient() throws HttpException, IOException {
		GetMethod method = new GetMethod(CPATH_SERVICE_URL + 
				"?version=2.0&q=BRCA2&output=xml&cmd=search");
		method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
				new DefaultHttpMethodRetryHandler());
		StringBuffer sb = new StringBuffer();
		int statusCode = client.executeMethod(method);
		if (statusCode == HttpStatus.SC_OK) {
			BufferedReader in = new BufferedReader(
				new InputStreamReader(method.getResponseBodyAsStream()));
			String line;
			while((line=in.readLine()) != null) {
				sb.append(line)
					.append(System.getProperty("line.separator"));
			}
		}
		method.releaseConnection();
		String result = sb.toString();
		assertTrue(result.length()>0);
	}
	
	
	/*
	 * TODO Command: get_pathways
	 * 
	 * http://www.pathwaycommons.org/pc/webservice.do?cmd=get_pathways&version=2.0&q=O14763&input_id_type=UNIPROT
	 * 
	 * [Required] cmd=get_pathways
	 * [Required] version=2.0
	 * [Required] q= a comma separated list of internal or external identifiers
	 * [Optional] parameter input_id_type. Valid values (IDType enum):
		UNIPROT
		CPATH_ID
		ENTREZ_GENE
		GENE_SYMBOL
	 * [Optional] parameter data_source. Values:
		BIOGRID
		CELL_MAP (not in MIRIAM yet)
		HPRD (iRefWeb)
		HUMANCYC
		IMID
		INTACT
		MINT
		NCI_NATURE
		REACTOME
	 * Note: datasource names depend on the pathwaycommons.org release;
	 * in the future, all Miriam's datatype names or synonyms should be welcomed;
	 * org.bridgedb.DataSource can be used to cache the list of supported ds...
	 *
	 * Output is a tab-delimited text file with four data columns.
	 */
	@Test
	public void testGetPathways() {
		
	}
	
	
	/*
	 * TODO Command: get_neighbors
	 * 
	 * http://www.pathwaycommons.org/pc/webservice.do?version=3.0&cmd=get_neighbors&q=9854
	 * 
	 * [Required] cmd=get_neighbors
	 * [Required] version=3.0 (this is not BioPAX Level!)
	 * [Required] q= an internal or external identifier (ID), corresponding to the physical entity of interest.
	 * [Optional] output= 
	    biopax (default) 
	    id_list
	    binary_sif
	    image_map
	    image_map_thumbnail
	    image_map_frameset
	 * [Optional] input_id_type= (use only with 'gsea' or 'binary_sif' formats; valid values - see Command: get_pathways)
	 * [Optional] output_id_type= (use only with 'gsea' or 'binary_sif' formats; valid values - same as for input_id_type)
	 * [Optional] data_source= Valid values - see above (Command: get_pathways)
	 * [Optional] binary_interaction_rule= (only when output=binary_sif) a comma separated list of 
	 * binary interaction rules that are applied when binary interactions are requested:
	   COMPONENT_OF
	   CO_CONTROL_DEPENDENT_ANTI
	   CO_CONTROL_DEPENDENT_SIMILAR
	   CO_CONTROL_INDEPENDENT_ANTI
	   CO_CONTROL_INDEPENDENT_SIMILAR
	   INTERACTS_WITH
	   IN_SAME_COMPONENT
	   METABOLIC_CATALYSIS
	   REACTS_WITH
	   SEQUENTIAL_CATALYSIS
	   STATE_CHANGE
	 */
	@Test
	public void testGetNeighbors() {
		
	}
	
	
	/*
	 * TODO Command: get_parents
	 * 
	 * http://www.pathwaycommons.org/pc/webservice.do?version=2.0&q=45202&output=xml&cmd=get_parents
	 * response xml (<summary_response/>) schema: http://pathway-commons.googlecode.com/hg/cpath2-mvn/cpath-web-service/src/main/resources/schemas/cpath1-responses.xsd
	 * (previously referred as http://www.pathwaycommons.org/pc/xml/SummaryResponse.xsd)
	 * 
	 * [Required] cmd=get_parents
	 * [Required] version=2.0
	 * [Required] q= an internal identifier, used to identify the physical entity or interaction of interest.
	 * [Required] output=xml
	 * 
	 * 
	 */
	@Test
	public void testGetParents() {
		
	}
	
	
	/*
	 * TODO Command: get_record_by_cpath_id
	 * 
	 * http://www.pathwaycommons.org/pc/webservice.do?cmd=get_record_by_cpath_id&version=2.0&q=1&output=biopax
	 * 
	 * [Required] cmd=get_record_by_cpath_id
	 * [Required] version=2.0
	 * [Required] q= a comma delimited list of internal identifiers, used to identify the pathways, interactions or physical entities of interest.
	 * [Required] output= biopax, binary_sif, gsea, or pc_gene_set.
	 * [Optional] output_id_type= (use only with 'gsea' or 'binary_sif' formats)
	 * [Optional] binary_interaction_rule= (only when output=binary_sif) a comma separated list of 
	 * binary interaction rules (see Command: 'get_neighbors').   
	 */
	@Test
	public void testRecordByCPathId() {
		
	}
	
	
	
	/*
	 *  TODO show how, using RestTemplate and custom implementation of the interface HttpMessageConverter<T>, 
	 *  to get a BioPAX element or Model from the web service! :)
	 */
}
