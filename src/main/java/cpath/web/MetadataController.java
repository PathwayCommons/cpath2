package cpath.web;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import javax.imageio.ImageIO;

import cpath.service.CPathUtils;
import cpath.service.metadata.Datasource;
import cpath.web.args.binding.MetadataTypeEditor;

import io.swagger.v3.oas.annotations.Hidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.*;

@Profile("web")
@Hidden
@RestController
@RequestMapping(method = {RequestMethod.GET})
public class MetadataController extends BasicController {

  private static final Logger log = LoggerFactory.getLogger(MetadataController.class);

  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.registerCustomEditor(Datasource.METADATA_TYPE.class, new MetadataTypeEditor());
  }

  @RequestMapping(value = "/metadata/logo/{identifier}", produces = {IMAGE_GIF_VALUE})
  public byte[] queryForLogo(@PathVariable String identifier)
    throws IOException {
    Datasource ds = service.metadata().findByIdentifier(identifier);
    byte[] bytes = null;

    if (ds != null) {
      BufferedImage bufferedImage = ImageIO
        .read(CPathUtils.LOADER.getResource(ds.getIconUrl()).getInputStream());

      //resize (originals are around 125X60)
      bufferedImage = scaleImage(bufferedImage, 100, 50);

      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      ImageIO.write(bufferedImage, "gif", byteArrayOutputStream);
      bytes = byteArrayOutputStream.toByteArray();
    }

    return bytes;
  }

  @RequestMapping(value = "/metadata", produces = {APPLICATION_JSON_VALUE})
  public Map<String, Object> queryForMetadata() {
    TreeMap<String, Object> props = new TreeMap();

    props.put("version", service.settings().getVersion());
    props.put("name", service.settings().getName());
    props.put("description", service.settings().getDescription());
    props.put("logo", service.settings().getLogo());
    props.put("xmlBase", service.settings().getXmlBase());
    props.put("url", service.settings().getUrl());
    props.put("organisms", service.settings().getOrganismsAsTaxonomyToNameMap());

    return props;
  }

  // to return a xml or json data http response
  @RequestMapping(value = "/metadata/datasources", produces = {APPLICATION_JSON_VALUE})
  public List<Datasource> queryForDatasources() {
    log.debug("Getting pathway datasources info.");
    //pathway/interaction data sources
    List<Datasource> ds = new ArrayList<>();
    //warehouse data sources
    List<Datasource> wh = new ArrayList<>();

    for (Datasource m : service.metadata().getDatasources()) {
      //set dynamic extra fields
      if (m.getType().isNotPathwayData()) {
        wh.add(m);
      } else {
        ds.add(m);
      }
    }

    //add warehouse data sources to the end of the list
    ds.addAll(wh);

    return ds;
  }

  @RequestMapping(value = "/metadata/datasources/{identifier}", produces = {APPLICATION_JSON_VALUE})
  public Datasource datasource(@PathVariable String identifier) {
    Datasource m = service.metadata().findByIdentifier(identifier);
    if (m == null)
      return null;
    return m;
  }

}