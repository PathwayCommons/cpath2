package cpath.service;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class WebMvcConfig extends WebMvcConfigurerAdapter {

    // Enable content negotiation;
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

    // Enable CORS globally; by default - all origins, GET, HEAD, POST
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**");
    }

//    @Bean
//    public static PropertySourcesPlaceholderConfigurer
//    propertySourcesPlaceholderConfigurer() {
//        return new PropertySourcesPlaceholderConfigurer();
//    }
}
