
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

import javax.xml.bind.*;
import javax.xml.transform.Source;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.junit.*;
import org.springframework.http.converter.*;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
//import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.web.client.RestTemplate;
import org.springframework.xml.transform.StringSource;

import cpath.webservice.jaxb.*;

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

	/*
	 * Command: search
	 * http://www.pathwaycommons.org/pc/webservice.do?version=2.0&q=BRCA2&format=xml&cmd=search
	 * response xml schema: http://www.pathwaycommons.org/pc/xml/SearchResponse.xsd
	 */
	@Test
	public void testSearchWithRestTemplate() throws JAXBException {
		String result = template.getForObject(CPATH_SERVICE_URL + 
				"?version=2.0&q=BRCA2&format=xml&cmd=search", String.class);
		assertNotNull(result);
		// Wow, we got some xml! Optionally, unmarshal to java bean, then do something...
		JAXBContext jaxbContext = JAXBContext.newInstance("cpath.webservice.jaxb");
		Unmarshaller un = jaxbContext.createUnmarshaller();
		JAXBElement<?> element = (JAXBElement<?>) un.unmarshal(new StringSource(result));
		Object response = element.getValue();
		if(response instanceof ErrorType) {
			System.out.println(((ErrorType)response).getErrorMsg());
		} else {
			//SearchResponseType response = unmarshaller.un(new StringSource(result), SearchResponseType.class).getValue();
			assertTrue(((SearchResponseType)response).getTotalNumHits()>0);
			System.out.println(((SearchResponseType)response).getSearchHit().toString());
		}
	}

	/*
	 * Another variant that gets the SearchResponseType! 
	 */
	@Test
	public void testSearchWithRestTemplate2() throws JAXBException {
		/* did not work...
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		//marshaller.setSchema(new ClassPathResource("schemas/cpath1-responses.xsd"));
		marshaller.setContextPath("cpath.webservice.jaxb");
		MarshallingHttpMessageConverter messageConverter = 	new MarshallingHttpMessageConverter(marshaller);
		messageConverter.setUnmarshaller(marshaller);
		template.getMessageConverters().add(messageConverter.setUnmarshaller);
		*/
		Source source = template.getForObject(CPATH_SERVICE_URL + 
				"?version=2.0&q=BRCA2&format=xml&cmd=search", Source.class);
		assertNotNull(source);
		JAXBContext jaxbContext = JAXBContext.newInstance("cpath.webservice.jaxb");
		// unlike before, we do not check for ErrorType here
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
				"?version=2.0&q=BRCA2&format=xml&cmd=search");
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
		
		System.out.println(result);
	}

	
	@Test
	public void testSearchWithHttpClient() throws HttpException, IOException {
		GetMethod method = new GetMethod(CPATH_SERVICE_URL + 
				"?version=2.0&q=BRCA2&format=xml&cmd=search");
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
	 * http://www.pathwaycommons.org/pc/webservice.do?cmd=get_pathways&version=2.0&q=O14763&input_id_type=UNIPROT
	 */

	
	/*
	 * TODO Command: get_neighbors
	 * http://www.pathwaycommons.org/pc/webservice.do?version=3.0&cmd=get_neighbors&q=9854
	 */
	
	
	/*
	 * TODO Command: get_parents
	 * http://www.pathwaycommons.org/pc/webservice.do?version=2.0&q=45202&output=xml&cmd=get_parents
	 * response xml schema: http://www.pathwaycommons.org/pc/xml/SummaryResponse.xsd
	 */
	
	
	/*
	 * TODO Command: get_record_by_cpath_id
	 * http://www.pathwaycommons.org/pc/webservice.do?cmd=get_record_by_cpath_id&version=2.0&q=1&output=biopax
	 * return formats: biopax, gsea, pc_gene_set, binary_sif
	 */
	
	
	/*
	 * TODO 
	 * 
	 */
	
		
	/*
	 *  TODO show how, using RestTemplate and custom implementation of the interface HttpMessageConverter<T>, 
	 *  to get a BioPAX element or Model from the web service! :)
	 */

	
	
	/*
	Valid values for the input_id_type parameter:
		UNIPROT
		CPATH_ID
		ENTREZ_GENE
		GENE_SYMBOL
	Valid values for the output_id_type parameter:
		UNIPROT
		CPATH_ID
		ENTREZ_GENE
		GENE_SYMBOL
	Valid values for the data_source parameter:
		BIOGRID
		CELL_MAP
		HPRD
		HUMANCYC
		IMID
		INTACT
		MINT
		NCI_NATURE
		REACTOME
	 */
}
