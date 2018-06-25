package cpath.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

  @Autowired
  private Settings cpath;

  @ModelAttribute("cpath")
  public Settings cpath() {
    return cpath;
  }
}
