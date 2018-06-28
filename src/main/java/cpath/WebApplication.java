package cpath;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

@SpringBootConfiguration
@EnableWebMvc
@Profile({"web"})
public class WebApplication implements WebMvcConfigurer
{

  // Enable content negotiation via
  // content-type (application/json, application/xml) or path extension (.json or .xml)
  @Override
  public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
    configurer
      .favorPathExtension(true)
      .ignoreUnknownPathExtensions(true)
      .favorParameter(false)
      .ignoreAcceptHeader(false)
      .useRegisteredExtensionsOnly(true)
      .defaultContentType(MediaType.APPLICATION_JSON)
      .mediaType("json", MediaType.APPLICATION_JSON)
      .mediaType("xml", MediaType.APPLICATION_XML)
    ;
  }

  // Enable CORS globally; by default - all origins, GET, HEAD, POST
  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**");
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
  }

  @Override
  public void configureViewResolvers(ViewResolverRegistry registry) {
    InternalResourceViewResolver resolver = new InternalResourceViewResolver();
    resolver.setPrefix("/jsp/");
    resolver.setSuffix(".jsp");
    resolver.setViewClass(JstlView.class);
    registry.viewResolver(resolver);
  }
}
