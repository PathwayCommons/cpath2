package cpath.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
//import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
//@EnableCaching(proxyTargetClass = true)
@PropertySource(value = "file:${CPATH2_HOME}/cpath2.properties", ignoreResourceNotFound = true)
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "cpath.jpa")
public class Application extends WebMvcConfigurerAdapter {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    //enable content negotiation -
    // mediaType: application/json, aplication/xml and pathExtension: .json and .xml
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.favorPathExtension(true).
                favorParameter(false).
                ignoreAcceptHeader(false).
                useJaf(false).
                defaultContentType(MediaType.APPLICATION_XML).
                mediaType("xml", MediaType.APPLICATION_XML).
                mediaType("json", MediaType.APPLICATION_JSON);
    }

    //enable CORS globally; by default - all origins, GET, HEAD, POST
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**");
    }

//    @Bean
//    public static PropertySourcesPlaceholderConfigurer
//    propertySourcesPlaceholderConfigurer() {
//        return new PropertySourcesPlaceholderConfigurer();
//    }

//TODO: handle error responses: 400, 404, 405, 406, 452, 460, 500, 503 (error.jsp)
}
