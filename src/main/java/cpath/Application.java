package cpath;

import cpath.service.ConsoleApplication;
import cpath.service.api.CPathService;
import cpath.web.WebApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Arrays;

@SpringBootApplication
public class Application {

  public static void main(String[] args) {

    SpringApplication application;

    if (args.length==0 || Arrays.stream(args)
      .anyMatch(ConsoleApplication.Cmd.SERVER.toString()::equalsIgnoreCase))
    {
      application = new SpringApplication(Application.class);
      application.setAdditionalProfiles("web");
    }
    else
    {
      application = new SpringApplication(ConsoleApplication.class);
      if (args[0].equals(ConsoleApplication.Cmd.PREMERGE.toString())) {
        //enable biopax-validator configuration
        application.setAdditionalProfiles("admin", "premerge");
      } else {
        application.setAdditionalProfiles("admin");
      }
    }

    ConfigurableApplicationContext applicationContext = application.run(args);
    CPathService service = applicationContext.getBean(CPathService.class);
    service.init();
  }

}
