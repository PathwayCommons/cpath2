package cpath.service;

import org.biopax.validator.ExceptionsAspect;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.annotation.*;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

//Override some objects in the biopax-validator's built-in XML context.
@Profile("premerge")
@Configuration
@EnableSpringConfigured //enables AOP
@ImportResource("classpath:META-INF/spring/appContext-validator.xml")
@ComponentScan(basePackages = {"org.biopax.validator.rules"})
public class BiopaxConfiguration {
  @Bean
  ReloadableResourceBundleMessageSource rulesMessageSource() {
    ReloadableResourceBundleMessageSource bean = new ReloadableResourceBundleMessageSource();
    //use validation settings from validation.properties instead of default profiles.properties
    bean.setBasenames("rules", "codes", "validation");
    return bean;
  }

  //Currently, there's no needed in replacing ontologyManager bean in the XML context -
  //- we don't actually use its CvRepository interface (e.g., to generate a CV from standard URI) TODO
  @Bean
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
