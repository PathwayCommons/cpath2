package cpath.web;

import cpath.service.Settings;
import cpath.service.api.GraphType;
import cpath.service.api.OutputFormat;
import cpath.service.metadata.Datasource;
import cpath.web.args.binding.*;
import org.biopax.paxtools.pattern.miner.SIFType;
import org.biopax.paxtools.query.algorithm.Direction;
import org.biopax.paxtools.query.algorithm.LimitType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.context.request.WebRequest;

@Profile("web")
@ControllerAdvice
public class GlobalControllerAdvice {

  @Autowired
  private Settings cpath;

  @ModelAttribute("cpath")
  public Settings cpath() {
    return cpath;
  }

  /*
   These unfortunately do not apply to a @RequestBody bean (json/xml) binding, mapping (HTTP POST);
   TODO: we changed to the bean property setters so that these custom editors might not be necessary anymore
   */
  @InitBinder
  public void registerCustomEditors(WebDataBinder binder, WebRequest request) {
    binder.registerCustomEditor(Class.class, new BiopaxTypeEditor());
    binder.registerCustomEditor(Datasource.METADATA_TYPE.class, new MetadataTypeEditor());
    binder.registerCustomEditor(OutputFormat.class, new OutputFormatEditor());
    binder.registerCustomEditor(Direction.class, new DirectionEditor());
    binder.registerCustomEditor(GraphType.class, new GraphTypeEditor());
    binder.registerCustomEditor(LimitType.class, new GraphQueryLimitEditor());
    binder.registerCustomEditor(SIFType.class, new SIFTypeEditor()); //also works for the SIFEnum subclass
  }
}
