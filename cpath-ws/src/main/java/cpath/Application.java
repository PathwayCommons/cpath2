package cpath;

import cpath.service.CPathService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);
        context.getBean(CPathService.class).init();
    }

    @Configuration
    @EnableWebMvc
    static class CPathWebConfigurer implements WebMvcConfigurer {
        // Enable content negotiation (for /search, /traverse. /top_pathways, /help commands);
        // mediaType: application/json, aplication/xml and pathExtension: .json and .xml
        public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
            configurer
                .favorPathExtension(true)
//                .ignoreUnknownPathExtensions(true)
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
}
