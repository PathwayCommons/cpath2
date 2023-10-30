package cpath.web;

import cpath.service.Settings;
import cpath.service.api.Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles({"web"})
@WebMvcTest(controllers = {HelpController.class})
@Import({WebApplication.class, Settings.class})
public class HelpControllerTest {
	@Autowired
	private MockMvc mvc;

	@MockBean
	private Service service;

	@Autowired
  private Settings settings;

  @BeforeEach
  public void init() {
    given(service.settings()).willReturn(settings);
  }

  @Test
	public void testGetTypes() throws Exception {
    mvc.perform(get("/help/types")) // default json
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().string(
			  containsString("{\"id\":\"types\",\"title\":\"BioPAX classes\",\"info\":\"These BioPAX Level3 classes")))
      .andExpect(content().string(
        containsString("{\"id\":\"TissueVocabulary\",\"title\":null,\"info\":null,\"example\":null,\"output\":null,\"members\":[],")));
	}

  @Test
  public void testGetTypesXml() throws Exception {
    mvc.perform(get("/help/types").accept(MediaType.APPLICATION_XML))
        .andExpect(status().is4xxClientError()); // only application/json is supported now
  }

  @Test
  public void testGetTypeJson() throws Exception {
    mvc.perform(get("/help/types/Gene").accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(content().json("{\"id\":\"Gene\",\"title\":\"Gene\",\"info\":\"See: biopax.org, " +
        "https://www.biopax.org/owldoc/Level3/\",\"example\":null,\"output\":null,\"members\":[],\"empty\":false}"));
  }

}
