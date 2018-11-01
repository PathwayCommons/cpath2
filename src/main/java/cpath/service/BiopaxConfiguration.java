package cpath.service;

import org.biopax.validator.ExceptionsAspect;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.annotation.*;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Profile("premerge")
@Configuration
@EnableSpringConfigured //enables AOP
@ImportResource("classpath:META-INF/spring/appContext-validator.xml")
@ComponentScan(basePackages = {"org.biopax.validator.rules"})
public class BiopaxConfiguration {

  //Replaces the ontologyManager from the orig. XML context conf.
  //to enable CvRepository interface (can generate a CV from URI,etc., but it's not used at the moment)
  @Bean
  @Scope("singleton")
  OntologyManager ontologyManager() throws IOException {
    PropertiesFactoryBean oboPropertiesFactoryBean = new PropertiesFactoryBean();
    oboPropertiesFactoryBean.setLocation(new ClassPathResource("obo.properties"));
    oboPropertiesFactoryBean.setLocalOverride(false);
    return new OntologyManager(oboPropertiesFactoryBean.getObject(), null);
  }

  @Bean
  ExceptionsAspect exceptionsAspect() {
    return new ExceptionsAspect();
  }

}
