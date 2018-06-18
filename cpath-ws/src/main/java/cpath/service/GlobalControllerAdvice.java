package cpath.service;

import cpath.config.CPathSettings;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {
  @ModelAttribute("cpath")
  public CPathSettings cpath() {
    return CPathSettings.getInstance();
  }
}
