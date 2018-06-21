package cpath;

import org.biopax.validator.impl.ExceptionsAspect;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.io.ClassPathResource;

@SpringBootConfiguration
@EnableConfigurationProperties(Settings.class)
@EnableSpringConfigured
@ImportResource("classpath:META-INF/spring/appContext-validator.xml")
@ComponentScan(basePackages = {"org.biopax.validator.rules"})
public class BiopaxConfig {

    //Override some beans in the biopax-validator's built-in Spring XML context configuration -

    @Bean
    ReloadableResourceBundleMessageSource rulesMessageSource () {
        ReloadableResourceBundleMessageSource bean = new ReloadableResourceBundleMessageSource();
        bean.setBasenames("rules","codes","validation");
        return bean;
    }

    @Bean
    //load bio ontology configuration from the XML resource
    //(to be used by the customized OntologyManager - OntologyManagerCvRepository - auto-configured bean)
    PropertiesFactoryBean oboPropertiesFactoryBean() {
        PropertiesFactoryBean bean = new PropertiesFactoryBean();
        bean.setLocation(new ClassPathResource("obo.properties"));
        bean.setLocalOverride(false);
        return bean;
    }

    @Bean
    ExceptionsAspect exceptionsAspect() {
        return new ExceptionsAspect();
    }

}
