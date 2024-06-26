package cpath.web;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import cpath.service.Settings;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
@Profile("web")
@Hidden //from swagger/openapi
@RequestMapping(method = RequestMethod.GET)
public class PagesController extends BasicController {

  @ModelAttribute("cpath")
  public Settings instance() {
    return service.settings();
  }

  @RequestMapping({"/", "/home"})
  public String home() {
    return "home";
  }

  @RequestMapping("/formats")
  public String formats() {
    return "formats";
  }

  @RequestMapping("/datasources")
  public String datasources() {
    return "datasources";
  }

  @RequestMapping("/favicon.ico")
  public @ResponseBody
  byte[] favicon() throws IOException {

    String cpathLogoUrl = service.settings().getLogo();

    byte[] iconData = null;

    BufferedImage image = ImageIO.read(new URL(cpathLogoUrl));
    if (image != null) {
      image = scaleImage(image, 16, 16);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(image, "gif", baos);
      baos.flush();
      iconData = baos.toByteArray();
    }

    return iconData;
  }

  @RequestMapping("/robots.txt")
  public @ResponseBody
  String robots() {
    // deny robots access to logs, web services and data files,
    // but allow - to web page resources (css, js, images)
    return "User-agent: *\n" +
      "Disallow: /v2\n" +
      "Disallow: /get\n" +
      "Disallow: /search\n" +
      "Disallow: /graph\n" +
      "Disallow: /top_pathways\n" +
      "Disallow: /traverse\n" +
      "Disallow: /archives\n" +
      "Disallow: /help\n" +
      "Disallow: /metadata\n";
  }

}