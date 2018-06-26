package cpath;

import cpath.service.ConsoleConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class Application {

  public static void main(String[] args) {

    SpringApplication application = new SpringApplication(Application.class);

    if (args[0].equals(ConsoleConfiguration.Cmd.PREMERGE.toString())) {
      application.setAdditionalProfiles("premerge"); //enables biopax-validator configuration
    }

    application.run(args);
  }
}
