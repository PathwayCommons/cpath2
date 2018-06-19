package cpath.console;

import cpath.service.OntologyManagerCvRepository;
import org.biopax.validator.impl.ExceptionsAspect;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.annotation.*;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Configuration
@EnableSpringConfigured
@ImportResource("classpath:META-INF/spring/appContext-validator.xml")
@ComponentScan(basePackages = {"org.biopax.validator.rules"})
public class BiopaxValidatorConfig {

    //Override some beans from the default biopax-validator xml config

    @Bean
    ReloadableResourceBundleMessageSource rulesMessageSource () {
        ReloadableResourceBundleMessageSource bean = new ReloadableResourceBundleMessageSource();
        bean.setBasenames("rules","codes","validation");
        return bean;
    }

    @Bean
    public PropertiesFactoryBean oboPropFactory() {
        PropertiesFactoryBean bean = new PropertiesFactoryBean();
        bean.setLocation(new ClassPathResource("obo.properties"));
        bean.setLocalOverride(false);
        return bean;
    }

    @Bean
    @Scope("singleton")
    OntologyManagerCvRepository ontologyManager() throws IOException {
        OntologyManagerCvRepository bean = new OntologyManagerCvRepository(oboPropFactory().getObject());
        return bean;
    }

    @Bean
    ExceptionsAspect exceptionsAspect() {
        return new ExceptionsAspect();
    }

}
