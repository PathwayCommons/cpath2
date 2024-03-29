package cpath.web;

import cpath.service.Settings;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("web")
public class OpenApiConfig {

  @Autowired
  Settings settings;

  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
      .info(apiInfo());
  }

  private Info apiInfo() {
    return new Info()
      .title("Bio pathway services")
      .description(settings.getDescription() + "</br>&copy; " + settings.getOrganization())
      .version(settings.getVersion())
      .contact(apiContact())
      .license(apiLicence());
  }

  private License apiLicence() {
    return new License()
      .name("MIT Licence").url("https://opensource.org/licenses/mit-license.php");
  }

  private Contact apiContact() {
    return new Contact()
      .name(settings.getOrganization())
      .email(settings.getEmail())
      .url(settings.getUrl());
  }

}
