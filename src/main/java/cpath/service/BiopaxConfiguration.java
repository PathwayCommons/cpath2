package cpath.service;

import org.springframework.context.annotation.*;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;

@Profile("premerge")
@Configuration
@EnableSpringConfigured //enables AOP
@ImportResource("classpath:META-INF/spring/appContext-validator.xml")
@ComponentScan(basePackages = {"org.biopax.validator.rules"})
public class BiopaxConfiguration {
}
