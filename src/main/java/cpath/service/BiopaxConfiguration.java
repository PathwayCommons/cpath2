package cpath.service;

import org.springframework.context.annotation.*;

@Configuration
@Profile("premerge")
@ImportResource({"classpath*:META-INF/spring/appContext-validator.xml",
  "classpath*:META-INF/spring/appContext-loadTimeWeaving.xml"})
public class BiopaxConfiguration {
}
