package cpath.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RestController;
import springfox.bean.validators.configuration.BeanValidatorPluginsConfiguration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Collections;

@Configuration
@Profile("web")
@EnableSwagger2
@Import(BeanValidatorPluginsConfiguration.class) //JSR-303 (if controllers have bean args and validation annotations)
public class SpringFoxConfig {

  @Autowired
  Settings settings;

  @Bean
  public Docket apiDocket() {
    return new Docket(DocumentationType.SWAGGER_2)
      .select()
//      .apis(RequestHandlerSelectors.any()) //this then shows Spring actuators, swagger, etc..
//      .apis(RequestHandlerSelectors.basePackage("cpath.service.web"))
      .apis(RequestHandlerSelectors.withClassAnnotation(RestController.class))
            .paths(PathSelectors.any())
      .build()
      .useDefaultResponseMessages(false)
      .apiInfo(getApiInfo());
  }

  private ApiInfo getApiInfo() {
    return new ApiInfo(
      settings.getName() + " " + settings.getVersion(),
      "cPath2 RESTful web services",
      "11",
      null,
      new Contact("Pathway Commons",
        "http://www.pathwaycommons.org",
        "pathway-commons-help@googlegroups.com"
      ),
      "MIT",
      "https://raw.githubusercontent.com/PathwayCommons/cpath2/master/LICENSE",
      Collections.emptyList()
    );
  }
}
