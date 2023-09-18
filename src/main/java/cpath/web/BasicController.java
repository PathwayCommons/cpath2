package cpath.web;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static cpath.service.api.Status.*;

import cpath.service.api.Service;
import cpath.service.ErrorResponse;
import cpath.service.api.OutputFormat;
import cpath.web.args.ServiceQuery;
import cpath.service.jaxb.*;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

/**
 * Basic controller.
 *
 * @author rodche
 */
public abstract class BasicController
{
  private static final Logger log = LoggerFactory.getLogger(BasicController.class);

  protected Service service;

  @Autowired
  public void setService(Service service) {
    this.service = service;
  }


  /*
   * Http error response with more details and specific access log events.
   */
  final void errorResponse(ServiceQuery args,
                                     ErrorResponse error,
                                     HttpServletRequest request,
                                     HttpServletResponse response)
  {
    // TODO: switch to using @RestControllerAdvice and @ExceptionHandler
    try {
      //log/track using a shorter message
      audit(request, args, null, error);
      //return a long detailed message
      response.sendError(error.getStatus().getCode(), error.getStatus().getCode() + "; " + error);
    } catch (IOException e) {
      log.error("FAILED sending an error response; " + e);
    }
  }


  /*
   * Builds an error message from
   * the web parameters binding result
   * if there are errors.
   */
  final String errorFromBindingResult(BindingResult bindingResult)
  {
    StringBuilder sb = new StringBuilder();
    for (FieldError fe : bindingResult.getFieldErrors()) {
      Object rejectedVal = fe.getRejectedValue();
      if (rejectedVal instanceof Object[]) {
        if (((Object[]) rejectedVal).length > 0) {
          rejectedVal = Arrays.toString((Object[]) rejectedVal);
        } else {
          rejectedVal = "empty array";
        }
      }
      sb.append(fe.getDefaultMessage()).append("; value: ").append(rejectedVal);
    }

    return sb.toString();
  }


  /*
   * Writes the query results to the HTTP response
   * output stream.
   */
  final void stringResponse(ServiceQuery command,
                                      ServiceResponse result,
                                      HttpServletRequest request,
                                      HttpServletResponse response)
  {
    if (result instanceof ErrorResponse) {
      errorResponse(command, (ErrorResponse) result, request, response);
    } else if (result instanceof DataResponse) {
      final DataResponse dataResponse = (DataResponse) result;

      // log/track one data access event for each data provider listed in the result
      audit(request, command, dataResponse.getProviders(), null);

      if (dataResponse.getData() instanceof Path) {
        //get the temp file
        Path resultFile = (Path) dataResponse.getData();
        try {
          response.setContentType(String
            .format("%s; %s", dataResponse.getFormat().getMediaType(), "charset=UTF-8"));
          long size = Files.size(resultFile);
          if (size > 13) { // a hack to skip for trivial/empty results
            Files.copy(resultFile, response.getOutputStream());
          }
        } catch (IOException e) {
          String msg = String.format("Failed to process the (temporary) result file %s; %s.", resultFile, e);
          errorResponse(command, new ErrorResponse(INTERNAL_ERROR, msg), request, response);
        } finally {
          try {
            Files.delete(resultFile);
          } catch (Exception e) {
            log.error(e.toString());
          }
        }
      } else if (dataResponse.isEmpty()) {
        //return empty string or trivial valid RDF/XML
        response.setContentType(dataResponse.getFormat().getMediaType());
        try {
          if (dataResponse.getFormat() == OutputFormat.BIOPAX) {
            //output an empty trivial BioPAX model
            Model emptyModel = BioPAXLevel.L3.getDefaultFactory().createModel();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            new SimpleIOHandler().convertToOWL(emptyModel, bos);
            response.getWriter().print(bos.toString("UTF-8"));
          }
          //else - SIF, GSEA formats do not allow for comment lines anyway
        } catch (IOException e) {
          String msg = String.format("Failed writing a trivial response: %s.", e);
          errorResponse(command, new ErrorResponse(INTERNAL_ERROR, msg), request, response);
        }
      } else { //it's probably a bug -
        String msg = String.format("BUG: DataResponse.data has value: %s, %s instead of a Path or null.",
          dataResponse.getData().getClass().getSimpleName(), dataResponse);
        errorResponse(command, new ErrorResponse(INTERNAL_ERROR, msg), request, response);
      }
    } else { //it's a bug -
      String msg = String.format("BUG: Unknown ServiceResponse: %s, %s ",
        result.getClass().getSimpleName(), result);
      errorResponse(command, new ErrorResponse(INTERNAL_ERROR, msg), request, response);
    }
  }


  /*
   * Resizes the image.
   */
  final BufferedImage scaleImage(BufferedImage img, int width, int height)
  {
    int imgWidth = img.getWidth();
    int imgHeight = img.getHeight();
    if (imgWidth * height < imgHeight * width) {
      width = imgWidth * height / imgHeight;
    } else {
      height = imgHeight * width / imgWidth;
    }
    BufferedImage newImage = new BufferedImage(width, height,
      BufferedImage.TYPE_INT_RGB);
    Graphics2D g = newImage.createGraphics();
    try {
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      g.clearRect(0, 0, width, height);
      g.drawImage(img, 0, 0, width, height, null);
    } finally {
      g.dispose();
    }
    return newImage;
  }


  /*
   * Extracts the client's IP from the request headers.
   */
//  private static String clientIpAddress(HttpServletRequest request)
//  {
//    String ip = request.getHeader("X-Forwarded-For");
//    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
//      ip = request.getHeader("Proxy-Client-IP");
//    }
//    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
//      ip = request.getHeader("WL-Proxy-Client-IP");
//    }
//    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
//      ip = request.getHeader("HTTP_CLIENT_IP");
//    }
//    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
//      ip = request.getHeader("HTTP_X_FORWARDED_FOR");
//    }
//    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
//      ip = request.getRemoteAddr();
//    }
//    return ip;
//  }

  void audit(HttpServletRequest request, ServiceQuery command, Set<String> providers, ErrorResponse err)
  {
    JsonObjectBuilder jb = Json.createObjectBuilder();

//get user-agent, IP, status, etc. from nginx/apache logs instead of here in the app...
//    jb.add("ip", clientIpAddress(request));

    if (err != null) {
      jb.add("error", err.toString());
    }

    if(command != null) {
      // TODO: change if there is any use (now we just add truncated string, not json object here
      //(can be very large if many URIs or SIF patterns are submitted in the request)
      jb.add("query", StringUtils.truncate(command.toString(),128));
    }

    if (!CollectionUtils.isEmpty(providers)) {
      jb.add("pro", Json.createArrayBuilder(providers));
    }

//    jb.add("accept", request.getHeader("Accept"));

    log.info(jb.build().toString());
  }

}