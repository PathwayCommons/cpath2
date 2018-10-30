package cpath.service.web;

import cpath.service.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@Profile("web")
@ControllerAdvice
public class GlobalControllerAdvice {

  @Autowired
  private Settings cpath;

  @ModelAttribute("cpath")
  public Settings cpath() {
    return cpath;
  }
}
