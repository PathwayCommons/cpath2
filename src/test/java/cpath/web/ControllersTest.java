package cpath.web;

import cpath.service.Settings;
import cpath.service.api.Service;
import cpath.service.jaxb.SearchHit;
import cpath.service.jaxb.SearchResponse;
import org.biopax.paxtools.model.level3.Pathway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles({"web"})
@WebMvcTest(controllers = {HelpController.class, BiopaxModelController.class})
@Import({WebApplication.class, Settings.class})
@AutoConfigureRestDocs
public class ControllersTest {
	@Autowired
	private MockMvc mvc;

	@MockBean
	private Service service;

	@Autowired
  private Settings settings;

  @BeforeEach
  public void init() {
    given(service.settings()).willReturn(settings);

    SearchHit mockHit = new SearchHit();
    mockHit.setBiopaxClass("Pathway");
    mockHit.setName("MockPathway");
    SearchResponse mockRes = new SearchResponse();
    mockRes.getSearchHit().add(mockHit);
    mockHit.setUri("MockUri");
    mockRes.setNumHits(1L);
    mockRes.setPageNo(0);
    mockRes.setComment("mock search result");

    given(service.search("Gly*",0, Pathway.class, null, null)).willReturn(mockRes);
  }

  @Test
	public void testGetTypes() throws Exception {
    mvc.perform(get("/help/types")) // default accept/expect json
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().string(
			  containsString("{\"id\":\"types\",\"title\":\"BioPAX classes\",\"info\":\"These BioPAX Level3 classes")))
      .andExpect(content().string(
        containsString("{\"id\":\"TissueVocabulary\",\"title\":null,\"info\":null,\"example\":null,\"output\":null,\"members\":[],")));
	}

  @Test
  public void testGetTypeJson() throws Exception {
    mvc.perform(get("/help/types/Gene").accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(content().json("{\"id\":\"Gene\",\"title\":\"Gene\",\"info\":\"See: biopax.org, " +
        "https://www.biopax.org/owldoc/Level3/\",\"example\":null,\"output\":null,\"members\":[],\"empty\":false}"));
  }

  @Test
  public void testGetTypeXml() throws Exception {
    mvc.perform(get("/help/types/Gene").accept(MediaType.APPLICATION_XML))
      .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_XML))
      .andExpect(content().xml("<?xml version=\"1.0\" encoding=\"UTF-8\" " +
        "standalone=\"yes\"?><help><id>Gene</id><info>See: biopax.org, " +
        "https://www.biopax.org/owldoc/Level3/</info><title>Gene</title></help>"));
  }

	@Test
	public void testSearchPathway() throws Exception {
    mvc.perform(get("/search?type=Pathway&q=Gly*"))
      .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
    .andExpect(content().string(containsString("MockPathway")));
	}
}
