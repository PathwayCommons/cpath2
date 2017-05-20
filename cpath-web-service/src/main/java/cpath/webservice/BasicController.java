package cpath.webservice;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static cpath.service.Status.*;

import cpath.config.CPathSettings;
import cpath.service.*;
import cpath.service.jaxb.*;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.jena.sparql.util.ModelUtils;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Provenance;
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
     * @param logEvents
     */
	protected final void errorResponse(Status status, String detailedMsg,
			HttpServletRequest request, HttpServletResponse response, Set<LogEvent> logEvents) {
		
		if(logEvents == null)
			logEvents = new HashSet<LogEvent>();
		
		// to count the error (code), also add -
		logEvents.add(LogEvent.error(status));
		
		//problems with logging subsystem should not fail the entire service
		try {
			service.log(logEvents, clientIpAddress(request));
		} catch (Throwable ex) {
			log.error("service.log failed" + ex);
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
	private final void errorResponse(Status status, String detailedMsg, HttpServletResponse response) {
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
	 * @param logEvents
	 */
	protected final void stringResponse(ServiceResponse serviceResp, HttpServletRequest request,
										HttpServletResponse response, Set<LogEvent> logEvents)
	{
		if(serviceResp instanceof ErrorResponse) {
			errorResponse(((ErrorResponse) serviceResp).getStatus(), serviceResp.toString(), request, response,
					logEvents);
		} 
		else if (serviceResp instanceof DataResponse) {
			final DataResponse dataResponse = (DataResponse) serviceResp;

			// take care to count provider's data accessed events
			Set<String> providers = dataResponse.getProviders();
			if(!providers.isEmpty())
				logEvents.addAll(LogEvent.providers(providers));
			
			//log to the db (for analysis and reporting)
			//problems with logging subsystem should not fail the entire service
			try {
				service.log(logEvents, clientIpAddress(request));
			} catch (Throwable ex) {
				log.error("service.log failed", ex);
			}

			if(dataResponse.getData() instanceof Path) {
				//get the temp file
				Path resultFile = (Path) dataResponse.getData();
				try {
					response.setHeader("Content-Length", String.valueOf(Files.size(resultFile)));
					response.setContentType(dataResponse.getFormat().getMediaType());
					Writer writer = response.getWriter();
					IOUtils.copyLarge(Files.newBufferedReader(resultFile), writer);
					writer.flush();
				} catch (IOException e) {
					errorResponse(INTERNAL_ERROR,
						String.format("Failed to process the (temporary) result file %s; %s.",
							resultFile, e.toString()), request, response, logEvents);
				} finally {
					try {Files.delete(resultFile);} catch (IOException e) {}
				}
			}
			else if(dataResponse.isEmpty()) {
				//return empty result (a trivial biopax rdf/xml or empty string)
				response.setContentType(dataResponse.getFormat().getMediaType());
				try {
					if(dataResponse.getFormat() == OutputFormat.BIOPAX) {
						//output an empty BioPAX model as RDF+XML
						Model emptyModel = BioPAXLevel.L3.getDefaultFactory().createModel();
						Provenance provenance = emptyModel.addNew(Provenance.class,
								CPathSettings.getInstance().getXmlBase()+CPathSettings.NO_DATA_FOUND);
						provenance.setDisplayName(CPathSettings.getInstance().getName());
						provenance.addComment("version:" + CPathSettings.getInstance().getVersion());
						provenance.addComment(CPathSettings.NO_DATA_FOUND);
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						new SimpleIOHandler().convertToOWL(emptyModel, bos);
						response.getWriter().print(bos.toString("UTF-8"));
					} else {
						//technically, SIF, GSEA formats do not have any comments
						response.getWriter().print(String.format("%s\\t%s\\t%s",
								CPathSettings.NO_DATA_FOUND,CPathSettings.NO_DATA_FOUND,CPathSettings.NO_DATA_FOUND));
					}
				} catch (IOException e) {
					errorResponse(INTERNAL_ERROR, String.format("Failed writing a 'no data found' response: %s.",
							e.toString()), request, response, logEvents);
				}
			}
			else { //it's probably a bug -
				errorResponse(INTERNAL_ERROR, String.format(
					"BUG: DataResponse.data has value: %s, %s instead of a Path or null.",
					dataResponse.getData().getClass().getSimpleName(), dataResponse.toString()),
						request, response, logEvents);
			}
		} else { //it's a bug -
			errorResponse(INTERNAL_ERROR, String.format("BUG: Unknown ServiceResponse: %s, %s ",
				serviceResp.getClass().getSimpleName(), serviceResp.toString()), request, response, logEvents);
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