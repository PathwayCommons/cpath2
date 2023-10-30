package cpath.web;

import cpath.service.Settings;
import cpath.service.api.Service;
import cpath.service.jaxb.SearchHit;
import cpath.service.jaxb.SearchResponse;
import org.biopax.paxtools.model.level3.Pathway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles({"web"})
@WebMvcTest(controllers = {ApiControllerV2.class})
@Import({WebApplication.class, Settings.class})
public class ApiControllerV2Test {
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

    //todo: mock data and add tests for: fetch, top_pathways, and graph queries
  }

  @Test
	public void getSearchPathway() throws Exception {
    mvc.perform(get("/v2/search?type=pathway&q=Gly*")).andExpect(status().isMethodNotAllowed());
	}

  @Test
  public void postSearchPathway() throws Exception {
    mvc.perform(post("/v2/search").content("""
        {
          "type": "pathway",
          "q": "Gly*"
        }
        """).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().string(containsString("MockPathway")));
  }

}
