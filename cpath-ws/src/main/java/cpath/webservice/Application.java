package cpath.webservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
//import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
//@EnableCaching(proxyTargetClass = true)
public class Application extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Application.class);
    }

    //TODO: configure context-path: /pc2

    //TODO: call CPathServiceImpl.init() in production mode (load the data)

    //TODO: configure content negotiation - also via extensions - '.json', '.xml'

    //TODO: configure CORS filter (allow from *)

    //TODO: handle error responses: 400, 404, 405, 406, 452, 460, 500, 503 (error.jsp)
}
