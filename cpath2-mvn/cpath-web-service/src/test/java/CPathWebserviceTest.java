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


@Ignore
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

	// by default - test cpath (legacy) webservices
	private String serviceUrl= CPATH_SERVICE_URL;
	
	// to use this class from another (cpath2 ws) test
	public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
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
	 * [Optional] output= xml (it's documented as 'required' but in fact - always ignored; format=xml also works...)
	 * [Optional] organism= organism filter. Must be specified as an NCBI Taxonomy identifier
	 */
	@Test
	public void testSearchWithRestTemplate() throws JAXBException {
		String result = template.getForObject(serviceUrl + 
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
		Source source = template.getForObject(serviceUrl + 
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
		URL url = new URL(serviceUrl + 
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
		GetMethod method = new GetMethod(serviceUrl + 
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
		URL url = new URL(serviceUrl
			+ "?version=2.0&cmd=get_pathways"
			+ "&input_id_type=UNIPROT&q=O14763&data_source=NCI_NATURE");
		BufferedReader in = new BufferedReader(new InputStreamReader(url
				.openStream()));
		StringBuffer result = new StringBuffer();
		String line;
		while ((line = in.readLine()) != null) {
			result.append(line).append(System.getProperty("line.separator"));
		}
		in.close();
		String data = result.toString();
		assertTrue(data.length() > 0);
		System.out.println(data);
	}
	
	
	@Test
	public void testGetPathwaysWrongId() throws IOException {
		URL url = new URL(serviceUrl
			+ "?version=2.0&cmd=get_pathways"
			+ "&input_id_type=UNIPROT&q=XXX");
		BufferedReader in = new BufferedReader(new InputStreamReader(url
				.openStream()));
		StringBuffer result = new StringBuffer();
		String line;
		while ((line = in.readLine()) != null) {
			result.append(line).append(System.getProperty("line.separator"));
		}
		in.close();
		String data = result.toString();
		assertTrue(data.length() > 0);
		System.out.println(data);
	}
	
	/*
	 * Command: get_neighbors
	 * 
	 * http://www.pathwaycommons.org/pc/webservice.do?version=3.0&cmd=get_neighbors&q=9854
	 * (9854 here - is the integer CPATH internal identifier; we don't have such in cpath2 - will use RDFId)
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
	 * [Optional] input_id_type= (valid values - see Command: get_pathways)
	 * [Optional] output_id_type= (use only with 'id_list' or 'binary_sif' formats; valid values - same as for input_id_type)
	 * [Optional] data_source= Valid values - see above (Command: get_pathways)
	 * [Optional] binary_interaction_rule= (only when output=binary_sif) a comma separated list of 
	 *  binary interaction rules that are applied when binary interactions are requested (BinaryInteractionType):
	    COMPONENT_OF, CO_CONTROL, INTERACTS_WITH, IN_SAME_COMPONENT, METABOLIC_CATALYSIS, REACTS_WITH
	    SEQUENTIAL_CATALYSIS, STATE_CHANGE
	 *   
	 * undocumented (!!): it may also return XML when there is no data and error happens to be error; 
	 * schema - http://pathway-commons.googlecode.com/hg/cpath2-mvn/cpath-web-service/src/main/resources/schemas/cpath1-responses.xsd 
	 *   
	 */
	@Test
	public void testGetNeighbors() throws IOException {
		URL url = new URL(serviceUrl
				+ "?version=3.0&cmd=get_neighbors" +
				"&q=9854" +
				"&input_id_type=CPATH_ID" +
				"&output_id_type=GENE_SYMBOL" +
				"&output=binary_sif" +
				"&binary_interaction_rule=INTERACTS_WITH" +
				"&data_source=HPRD,BIOGRID");
			BufferedReader in = new BufferedReader(new InputStreamReader(url
					.openStream()));
			StringBuffer result = new StringBuffer();
			String line;
			while ((line = in.readLine()) != null) {
				result.append(line).append(System.getProperty("line.separator"));
			}
			in.close();
			String data = result.toString();
			assertTrue(data.length() > 0);
			System.out.println(data);
	}
	
	
	@Test
	public void testGetNeighborsWrongArg() throws IOException {
		URL url = new URL(serviceUrl
				+ "?version=3.0&cmd=get_neighbors" +
				"&q=9854" +
				"&output=gsea"); // 'gsea' is wrong
			BufferedReader in = new BufferedReader(new InputStreamReader(url
					.openStream()));
			StringBuffer result = new StringBuffer();
			String line;
			while ((line = in.readLine()) != null) {
				result.append(line).append(System.getProperty("line.separator"));
			}
			in.close();
			String data = result.toString();
			assertTrue(data.length() > 0);
			System.out.println(data);
	}

	
	/*
	 * Command: get_parents
	 * 
	 * http://www.pathwaycommons.org/pc/webservice.do?version=2.0&q=45202&output=xml&cmd=get_parents
	 * response xml (<summary_response/>) schema: http://pathway-commons.googlecode.com/hg/cpath2-mvn/cpath-web-service/src/main/resources/schemas/cpath1-responses.xsd
	 * (previously referred as http://www.pathwaycommons.org/pc/xml/SummaryResponse.xsd)
	 * 
	 * [Required] cmd=get_parents
	 * [Required] version=2.0
	 * [Required] q= an internal identifier, used to identify the physical entity or interaction of interest.
	 * [Optional] output=xml (default and the only option is 'xml') In the WS docs it's specified as "Required", but in fact it is ignored.
	 * 
	 * 
	 */
	@Test
	public void testGetParents() throws IOException {
		URL url = new URL(serviceUrl
				+ "?version=2.0&cmd=get_parents&q=45202");
			BufferedReader in = new BufferedReader(new InputStreamReader(url
					.openStream()));
			StringBuffer result = new StringBuffer();
			String line;
			while ((line = in.readLine()) != null) {
				result.append(line).append(System.getProperty("line.separator"));
			}
			in.close();
			String data = result.toString();
			assertTrue(data.length() > 0);
			System.out.println(data);
	}
	
	
	/*
	 * TODO Command: get_record_by_cpath_id
	 * 
	 * http://www.pathwaycommons.org/pc/webservice.do?cmd=get_record_by_cpath_id&version=2.0&q=1&output=biopax
	 * 
	 * [Required] cmd=get_record_by_cpath_id
	 * [Required] version=2.0
	 * [Required] q= a comma delimited list of internal identifiers, used to identify the pathways, interactions or physical entities of interest.
	 * [Optional] output= biopax (default), binary_sif, gsea, or pc_gene_set. // modified: it's "required" in the original web service!
	 * [Optional] output_id_type= (use only with 'gsea' or 'binary_sif' formats)
	 * [Optional] binary_interaction_rule= (only when output=binary_sif) a comma separated list of 
	 * binary interaction rules (see Command: 'get_neighbors').   
	 */
	@Test
	public void testRecordByCPathId() throws IOException {
		URL url = new URL(serviceUrl
				+ "?version=2.0&cmd=get_record_by_cpath_id" +
				"&q=1&output=biopax");
			BufferedReader in = new BufferedReader(new InputStreamReader(url
					.openStream()));
			StringBuffer result = new StringBuffer();
			String line;
			while ((line = in.readLine()) != null) {
				result.append(line).append(System.getProperty("line.separator"));
			}
			in.close();
			String data = result.toString();
			assertTrue(data.length() > 0);
			System.out.println(data);
	}
	
	
	
	/*
	 *  TODO show how, using RestTemplate and custom implementation of the interface HttpMessageConverter<T>, 
	 *  to get a BioPAX element or Model from the web service! :)
	 */
}
