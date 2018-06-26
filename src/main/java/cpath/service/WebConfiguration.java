package cpath.service;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
@Profile({"web"})
public class WebConfiguration implements WebMvcConfigurer {
    // Enable content negotiation (for /search, /traverse. /top_pathways, /help commands);
    // mediaType: application/json, aplication/xml and pathExtension: .json and .xml
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
      configurer
        .favorPathExtension(true)
//        .ignoreUnknownPathExtensions(true)
        .favorParameter(false)
        .ignoreAcceptHeader(false)
        .useRegisteredExtensionsOnly(true)
        .defaultContentType(MediaType.APPLICATION_JSON)
        .mediaType("xml", MediaType.APPLICATION_XML)
        .mediaType("json", MediaType.APPLICATION_JSON)
      ;
    }

    // Enable CORS globally; by default - all origins, GET, HEAD, POST
    public void addCorsMappings(CorsRegistry registry) {
      registry.addMapping("/**");
    }
}
