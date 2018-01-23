package cpath.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static cpath.service.Status.*;

import cpath.service.jaxb.*;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

/**
 * Basic controller.
 * 
 * @author rodche
 */
public abstract class BasicController {
    private static final Logger log = LoggerFactory.getLogger(BasicController.class);
    
    protected CPathService service;  
    
    @Autowired
    public void setLogRepository(CPathService service) {
    	Assert.notNull(service,"'service' was null");
		this.service = service;
	}
    
    /**
     * Http error response with more details and specific access log events.
     * @param status
     * @param detailedMsg
	 * @param request
	 * @param response
	 * @param event
	 */
	protected final void errorResponse(Status status, String detailedMsg,
									   HttpServletRequest request, HttpServletResponse response,
									   JSONObject event)
	{
		final String msg = status.getMsg() + "; " + detailedMsg;

		if(event == null) event = new JSONObject();

		event.put("status", status.getCode());
		if(log.isDebugEnabled()) event.put("error", msg);
		event.put("uip", clientIpAddress(request));

		//logging/tracking should not cause service fail
		try {
			service.track(event); //for service and data access analytics
			response.sendError(status.getCode(), msg);
		} catch (Throwable e) {
			log.error("BUG: logging threw an exception" + e);
		}
	}


	/**
	 * Builds an error message from  
	 * the web parameters binding result
	 * if there're errors.
	 * 
	 * @param bindingResult
	 * @return
	 */
	protected final String errorFromBindingResult(BindingResult bindingResult) {
		StringBuilder sb = new StringBuilder();
		for (FieldError fe : bindingResult.getFieldErrors()) {
			Object rejectedVal = fe.getRejectedValue();
			if(rejectedVal instanceof Object[]) {
				if(((Object[]) rejectedVal).length > 0) {
					rejectedVal = Arrays.toString((Object[])rejectedVal);
				} else {
					rejectedVal = "empty array";
				}
			}
			sb.append(fe.getField() + " was '" + rejectedVal + "'; "
					+ fe.getDefaultMessage() + ". ");
		}
		
		return sb.toString();
	}
    
	
	/**
	 * Writes the query results to the HTTP response
	 * output stream.
	 * @param result
	 * @param request
	 * @param response
	 * @param event
	 */
	protected final void stringResponse(ServiceResponse result, HttpServletRequest request,
										HttpServletResponse response, JSONObject event)
	{
		if(result instanceof ErrorResponse) {
			errorResponse(((ErrorResponse) result).getStatus(), result.toString(), request, response, event);
		} 
		else if (result instanceof DataResponse) {
			final DataResponse dataResponse = (DataResponse) result;

			event.put("uip", clientIpAddress(request));
			JSONArray providers = new JSONArray();
			providers.addAll(dataResponse.getProviders());
			event.put("provider", providers);

			//problems with logging subsystem should not fail the entire service
			try {
				service.track(event);
			} catch (Throwable ex) {
				log.error("service.log failed", ex);
			}

			if(dataResponse.getData() instanceof Path) {
				//get the temp file
				Path resultFile = (Path) dataResponse.getData();
				try {
					response.setContentType(dataResponse.getFormat().getMediaType());
					long size = Files.size(resultFile);
					if(size > 13) { // TODO: why, a hack to skip for trivial/empty results
						response.setHeader("Content-Length", String.valueOf(size));
						Writer writer = response.getWriter();
						BufferedReader bufferedReader = Files.newBufferedReader(resultFile);
						try {
							IOUtils.copyLarge(bufferedReader, writer);
						} finally {
							bufferedReader.close();
						}
					}
				} catch (IOException e) {
					errorResponse(INTERNAL_ERROR,
						String.format("Failed to process the (temporary) result file %s; %s.",
							resultFile, e.toString()), request, response, event);
				} finally {
					try {Files.delete(resultFile);}catch(Exception e){log.error(e.toString());}
				}
			}
			else if(dataResponse.isEmpty()) {
				//return empty string or trivial valid RDF/XML
				response.setContentType(dataResponse.getFormat().getMediaType());
				try {
					if(dataResponse.getFormat() == OutputFormat.BIOPAX) {
						//output an empty trivial BioPAX model
						Model emptyModel = BioPAXLevel.L3.getDefaultFactory().createModel();
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						new SimpleIOHandler().convertToOWL(emptyModel, bos);
						response.getWriter().print(bos.toString("UTF-8"));
					} else {
						//SIF, GSEA formats do not allow for comment lines
//						response.getWriter().print(""); //nothing
					}
				} catch (IOException e) {
					errorResponse(INTERNAL_ERROR, String.format("Failed writing 'no data found' response: %s.",
							e.toString()), request, response, event);
				}
			}
			else { //it's probably a bug -
				errorResponse(INTERNAL_ERROR, String.format(
					"BUG: DataResponse.data has value: %s, %s instead of a Path or null.",
					dataResponse.getData().getClass().getSimpleName(), dataResponse.toString()),
						request, response, event);
			}
		} else { //it's a bug -
			errorResponse(INTERNAL_ERROR, String.format("BUG: Unknown ServiceResponse: %s, %s ",
				result.getClass().getSimpleName(), result.toString()), request, response, event);
		}
	}

	
	/**
	 * Resizes the image.
	 * 
	 * @param img
	 * @param width
	 * @param height
	 * @param background
	 * @return
	 */
	public final BufferedImage scaleImage(BufferedImage img, int width, int height,
	        Color background) {
	    int imgWidth = img.getWidth();
	    int imgHeight = img.getHeight();
	    if (imgWidth*height < imgHeight*width) {
	        width = imgWidth*height/imgHeight;
	    } else {
	        height = imgHeight*width/imgWidth;
	    }
	    BufferedImage newImage = new BufferedImage(width, height,
	            BufferedImage.TYPE_INT_RGB);
	    Graphics2D g = newImage.createGraphics();
	    try {
	        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
	                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
	        if(background != null)
	        	g.setBackground(background);
	        g.clearRect(0, 0, width, height);
	        g.drawImage(img, 0, 0, width, height, null);
	    } finally {
	        g.dispose();
	    }
	    return newImage;
	}
	
	
	/**
	 * Extracts the client's IP from the request headers.
	 * 
	 * @param request
	 * @return
	 */
	public static final String clientIpAddress(HttpServletRequest request) {
		
		String ip = request.getHeader("X-Forwarded-For");		
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getHeader("Proxy-Client-IP");  
        }  
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getHeader("WL-Proxy-Client-IP");  
        }  
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getHeader("HTTP_CLIENT_IP");  
        }  
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");  
        }  
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getRemoteAddr();  
        }  
		
        return ip;
	}
}