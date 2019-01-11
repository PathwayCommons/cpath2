package cpath;

import cpath.service.ConsoleApplication;
import cpath.service.api.CPathService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class Application {

  public static void main(String[] args) {

    SpringApplication application;

    if (args.length==0) {
      application = new SpringApplication(Application.class);
      application.setAdditionalProfiles("web");
      ConfigurableApplicationContext applicationContext = application.run(args);
      CPathService service = applicationContext.getBean(CPathService.class);
      service.init();
    } else {
      application = new SpringApplication(ConsoleApplication.class);
      if (args[0].equals(ConsoleApplication.Cmd.BUILD.toString())) {
        //"premerge" enables biopax-validator context
        application.setAdditionalProfiles("admin", "premerge");
      } else {
        application.setAdditionalProfiles("admin");
      }
      ConfigurableApplicationContext applicationContext = application.run(args);
      applicationContext.close();
    }
  }

}
