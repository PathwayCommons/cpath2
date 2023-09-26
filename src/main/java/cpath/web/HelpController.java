package cpath.web;

import cpath.service.api.GraphType;
import cpath.service.api.OutputFormat;
import cpath.service.jaxb.*;
import cpath.web.args.binding.*;

import io.swagger.v3.oas.annotations.Hidden;
import org.biopax.paxtools.controller.EditorMap;
import org.biopax.paxtools.controller.PropertyEditor;
import org.biopax.paxtools.controller.SimpleEditorMap;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.query.algorithm.Direction;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * Help Controller (returns JSON docs).
 * {@link Help} bean.
 */
@Profile("web")
@Hidden
@RestController
@RequestMapping(method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
public class HelpController extends BasicController {

  /**
   * Customizes request parameters conversion to proper internal types,
   * e.g., "network of interest" is recognized as GraphType.NETWORK_OF_INTEREST, etc.
   *
   * @param binder
   */
  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.registerCustomEditor(OutputFormat.class, new OutputFormatEditor());
    binder.registerCustomEditor(GraphType.class, new GraphTypeEditor());
    binder.registerCustomEditor(Direction.class, new DirectionEditor());
    binder.registerCustomEditor(Class.class, new BiopaxTypeEditor());
  }

  @RequestMapping("/help/schema")
  public void getSchema(HttpServletResponse response) throws Exception {
    response.setContentType("application/xml");
    Path xsdPath = Paths.get(ResourceUtils.getURL("classpath:cpath-schema.xsd").toURI());
    Files.copy(xsdPath, response.getOutputStream());
  }

  @RequestMapping("/help/formats")
  public Help getFormats() {
    Help help = new Help();
    help.setId("formats");
    help.setTitle("Output Formats");
    help.setInfo("can convert BioPAX to several text formats");
    help.setExample("help/formats/sif");
    for (OutputFormat f : OutputFormat.values())
      help.addMember(getFormat(f));

    return help;
  }

  @RequestMapping("/help/formats/{fmt}")
  public Help getFormat(@PathVariable OutputFormat fmt) {
    if (fmt == null) return getFormats();
    Help help = new Help();
    help.setId(fmt.name());
    help.setTitle(fmt.name());
    help.setInfo(fmt.getInfo());
    return help;
  }

  @RequestMapping("/help/types")
  public Help getBiopaxTypes() {
    Help help = new Help();

    for (Class<? extends BioPAXElement> t :
      SimpleEditorMap.L3.getKnownSubClassesOf(BioPAXElement.class)) {
      if (BioPAXLevel.L3.getDefaultFactory().getImplClass(t) != null) {
        help.addMember(new Help(t.getSimpleName()));
      }
    }
    help.setId("types");
    help.setTitle("BioPAX classes");
    help.setInfo("These BioPAX Level3 classes (including some abstract) can be used in search/traverse queries " +
        "(case insensitive):");
    help.setExample("search?type=pathway&q=b*");
    return help;
  }


  @RequestMapping("/help/types/{type}")
  public Help getBiopaxType(@PathVariable Class<? extends BioPAXElement> type) {
    if (type == null) return getBiopaxTypes();

    Help h = new Help(type.getSimpleName());
    h.setTitle(type.getSimpleName());
    h.setInfo("See: biopax.org, https://www.biopax.org/owldoc/Level3/");

    return h;
  }


  @RequestMapping("/help/types/{type}/properties")
  public Help getBiopaxTypeProperties(@PathVariable Class<? extends BioPAXElement> type) {
    final String id = type.getSimpleName() + " properties";
    Help h = new Help(id);
    h.setTitle(id);
    h.setInfo("BioPAX properties " +
      "for class: " + type.getSimpleName());

    EditorMap em = SimpleEditorMap.get(BioPAXLevel.L3);
    for (PropertyEditor e : em.getEditorsOf(type))
      h.addMember(new Help(e.toString()));

    return h;
  }

  @RequestMapping("/help/types/properties")
  public Help getBiopaxTypesProperties() {
    Help h = new Help("properties");
    h.setTitle("BioPAX Properites");
    h.setInfo("The list of all BioPAX properties");

    for (Class<? extends BioPAXElement> t :
      SimpleEditorMap.L3.getKnownSubClassesOf(BioPAXElement.class)) {
      if (BioPAXLevel.L3.getDefaultFactory().getImplClass(t) != null) {
        for (Help th : getBiopaxTypeProperties(t).getMembers()) {
          h.addMember(th);
        }
      }
    }

    return h;
  }

  @RequestMapping("/help/types/{type}/inverse_properties")
  public Help getBiopaxTypeInverseProperties(@PathVariable Class<? extends BioPAXElement> type) {
    final String id = type.getSimpleName() + " inverse_properties";
    Help h = new Help(id);
    h.setTitle(id);
    h.setInfo("Paxtools inverse properties " +
      "for class: " + type.getSimpleName());

    EditorMap em = SimpleEditorMap.get(BioPAXLevel.L3);
    for (PropertyEditor e : em.getInverseEditorsOf(type))
      h.addMember(new Help(e.toString()));

    return h;
  }

  @RequestMapping("/help/types/inverse_properties")
  public Help getBiopaxTypesInverseProperties() {
    Help h = new Help("inverse_properties");
    h.setTitle("Paxtools inverse properites");
    h.setInfo("The list of all inverse (Paxtools) properties");

    for (Class<? extends BioPAXElement> t :
      SimpleEditorMap.L3.getKnownSubClassesOf(BioPAXElement.class)) {
      if (BioPAXLevel.L3.getDefaultFactory().getImplClass(t) != null) {
        for (Help th : getBiopaxTypeInverseProperties(t).getMembers()) {
          h.addMember(th);
        }
      }
    }

    return h;
  }

  @RequestMapping("/help/kinds")
  public Help getGraphTypes() {
    Help help = new Help();
    for (GraphType type : GraphType.values()) {
      help.addMember(getGraphType(type));
    }
    help.setId("kinds");
    help.setTitle("Advanced Graph Query Types");
    help.setInfo("has the following built-in algorithms:");
    help.setExample("help/kinds/neighborhood");
    return help;
  }

  @RequestMapping("/help/kinds/{kind}")
  public Help getGraphType(@PathVariable GraphType kind) {
    if (kind == null) return getGraphTypes();
    Help help = new Help();
    help.setTitle(kind.name());
    help.setId(kind.name());
    help.setInfo(kind.getDescription());
    return help;
  }

  @RequestMapping("/help/directions")
  public Help getDirectionTypes() {
    Help help = new Help();
    for (Direction direction : Direction.values()) {
      help.addMember(getDirectionType(direction));
    }
    help.setId("directions");
    help.setTitle("Graph Query Traversal Directions");
    help.setInfo("Following are possible query directions:");
    help.setExample("help/directions/downstream");
    return help;
  }

  @RequestMapping("/help/directions/{direction}")
  public Help getDirectionType(@PathVariable Direction direction) {
    if (direction == null) return getDirectionTypes();
    Help help = new Help();
    help.setTitle(direction.name());
    help.setId(direction.name());
    help.setInfo(direction.getDescription());
    return help;
  }

}