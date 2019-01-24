package cpath;

import cpath.service.api.CPathService;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class Application {
  private static final Logger LOG = LoggerFactory.getLogger(Application.class);
  public static void main(String[] args) {
    SpringApplication application = new SpringApplication(Application.class);
    if (ArrayUtils.contains(args, "-s") || ArrayUtils.contains(args, "--server")) {
      LOG.info("Starting the web application...");
      application.setAdditionalProfiles("web");
      ConfigurableApplicationContext applicationContext = application.run(args);
      CPathService service = applicationContext.getBean(CPathService.class);
      service.init();
    } else {
      application.setWebApplicationType(WebApplicationType.NONE);
      if (ArrayUtils.contains(args, "-b") || ArrayUtils.contains(args, "--build")) {
        //enable biopax-validator context
        application.setAdditionalProfiles("admin", "premerge");
      } else {
        application.setAdditionalProfiles("admin");
      }
      ConfigurableApplicationContext applicationContext = application.run(args);
      applicationContext.close();
    }
  }

}
