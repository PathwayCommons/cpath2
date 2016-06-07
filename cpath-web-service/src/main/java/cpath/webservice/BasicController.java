package cpath.webservice;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static cpath.service.Status.*;
import cpath.jpa.LogEvent;
import cpath.service.CPathService;
import cpath.service.ErrorResponse;
import cpath.service.Status;
import cpath.service.jaxb.*;

import org.apache.commons.io.IOUtils;
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
    	Assert.notNull(service);  	
		this.service = service;
	}
    
    /**
     * Http error response with more details and specific access log events.
     * 
     * @param status
     * @param detailedMsg
     * @param request
     * @param response
     * @param updateCountsFor
     * @throws IOException
     */
	protected final void errorResponse(Status status, String detailedMsg,
			HttpServletRequest request, HttpServletResponse response, Set<LogEvent> updateCountsFor) {
		
		if(updateCountsFor == null)
			updateCountsFor = new HashSet<LogEvent>();
		
		// to count the error (code), also add -
		updateCountsFor.add(LogEvent.from(status));
		
		//problems with logging subsystem should not fail the entire service
		try {
			service.log(updateCountsFor, clientIpAddress(request));
		} catch (Throwable ex) {
			log.error("LogUtils.log failed" + ex);
		}

		errorResponse(status, status.getErrorMsg() + "; " + detailedMsg, response);
	}

	/**
	 * Simple http error response.
	 *
	 * @param status
	 * @param detailedMsg
	 * @param response
	 */
	protected final void errorResponse(Status status, String detailedMsg, HttpServletResponse response) {
		try {
			log.warn(status.getErrorCode() + "; " + status.getErrorMsg() + "; " + detailedMsg);
			response.sendError(status.getErrorCode(), status.getErrorMsg() + "; " + detailedMsg);
		} catch (Exception e) {
			log.error("errorResponse: response.sendError failed" + e);
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
	 * 
	 * @param serviceResp
	 * @param request
	 * @param response
	 * @param updateCountsFor
	 * @throws IOException
	 */
	protected final void stringResponse(ServiceResponse serviceResp, HttpServletRequest request,
										HttpServletResponse response, Set<LogEvent> updateCountsFor)
	{
		if(serviceResp instanceof ErrorResponse) {
			errorResponse(((ErrorResponse) serviceResp).getStatus(), serviceResp.toString(), request, response,
					updateCountsFor);
		} 
		else if(serviceResp.isEmpty()) {
			log.warn("stringResponse: I got an empty ServiceResponce " +
				"(must be already converted to the ErrorResponse)");
			errorResponse(NO_RESULTS_FOUND, "no results found", 
					request, response, updateCountsFor);
		} 
		else if (serviceResp instanceof DataResponse) {
			final DataResponse dataResponse = (DataResponse) serviceResp;

			// take care to count provider's data accessed events
			Set<String> providers = dataResponse.getProviders();
			updateCountsFor.addAll(LogEvent.fromProviders(providers));
			
			//log to the db (for analysis and reporting)
			//problems with logging subsystem should not fail the entire service
			try {
				service.log(updateCountsFor, clientIpAddress(request));
			} catch (Throwable ex) {
				log.error("LogUtils.log failed", ex);
			}
			
			if(dataResponse.getData() instanceof Path) {
				File resultFile = ((Path) dataResponse.getData()).toFile();//this is some temp. file
				response.setHeader("Content-Length", String.valueOf(resultFile.length()));
				response.setContentType(dataResponse.getFormat().getMediaType());
				log.debug("QUERY RETURNED " + dataResponse.getData().toString().length() + " chars");
				try {
					FileReader reader = new FileReader(resultFile);
					Writer writer = response.getWriter();
					IOUtils.copyLarge(reader, writer);
					writer.flush();
					reader.close();
				} catch (IOException e) {
					errorResponse(INTERNAL_ERROR, String.format("Failed to process the (temporary) result file %s; %s.",
						resultFile.getPath(), e.toString()), request, response, updateCountsFor);
				} finally {
					resultFile.delete();
				}
			} else { //it's probably a re-factoring bug -
				errorResponse(INTERNAL_ERROR, String.format("BUG: no file Path in the DataResponse; got %s, %s instead.",
					dataResponse.getData().getClass().getSimpleName(), dataResponse.toString()), request, response,
						updateCountsFor);
			}
		} else { //it's a bug -
			errorResponse(INTERNAL_ERROR, String.format("BUG: Unknown ServiceResponse: %s, %s ",
				serviceResp.getClass().getSimpleName(), serviceResp.toString()), request, response, updateCountsFor);
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