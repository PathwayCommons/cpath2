/**
 ** Copyright (c) 2010 Memorial Sloan-Kettering Cancer Center (MSKCC)
 ** and University of Toronto (UofT).
 **
 ** This is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** any later version.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** both UofT and MSKCC have no obligations to provide maintenance, 
 ** support, updates, enhancements or modifications.  In no event shall
 ** UofT or MSKCC be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** UofT or MSKCC have been advised of the possibility of such damage.  
 ** See the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this software; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA;
 ** or find it at http://www.fsf.org/ or http://www.gnu.org.
 **/

package cpath.webservice;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static cpath.service.Status.*;
import cpath.log.LogUtils;
import cpath.log.jpa.Geoloc;
import cpath.log.jpa.LogEntitiesRepository;
import cpath.log.jpa.LogEvent;
import cpath.service.ErrorResponse;
import cpath.service.Status;
import cpath.service.jaxb.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

/**
 * Basic controller.
 * 
 * @author rodche
 */
public abstract class BasicController {
    private static final Logger log = LoggerFactory.getLogger(BasicController.class);
    
    protected LogEntitiesRepository logEntitiesRepository;  
    
    @Autowired
    public void setLogRepository(LogEntitiesRepository logEntitiesRepository) {
		this.logEntitiesRepository = logEntitiesRepository;
	}
 
    
    /**
     * Http error response from the error bean.
     * 
     * @param status
     * @param detailedMsg
     * @param request
     * @param response
     * @param updateCountsFor
     * @throws IOException
     */
	protected final void errorResponse(Status status, String detailedMsg,
			HttpServletRequest request, HttpServletResponse response, Set<LogEvent> updateCountsFor) 
					throws IOException {
		
		if(updateCountsFor == null)
			updateCountsFor = new HashSet<LogEvent>();
		
		// to count the error (code), also add -
		updateCountsFor.add(LogEvent.from(status));
		
		LogUtils.log(logEntitiesRepository, 
				updateCountsFor, Geoloc.fromIpAddress(clientIpAddress(request)));
		
		response.sendError(status.getErrorCode(), 
			status.getErrorMsg() + "; " + detailedMsg);
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
	 * @param resp
	 * @param writer
	 * @param request
	 * @param response
	 * @param updateCountsFor
	 * @throws IOException
	 */
	protected final void stringResponse(ServiceResponse resp, 
			Writer writer, HttpServletRequest request, 
			HttpServletResponse response, Set<LogEvent> updateCountsFor) throws IOException 
	{
		if(resp instanceof ErrorResponse) {
			
			errorResponse(((ErrorResponse) resp).getStatus(), 
					((ErrorResponse) resp).toString(), request, response, updateCountsFor);
			
		} 
		else if(resp.isEmpty()) {
			log.warn("stringResponse: I got an empty ServiceResponce " +
				"(must be already converted to the ErrorResponse)");
			
			errorResponse(NO_RESULTS_FOUND, "no results found", 
					request, response, updateCountsFor);
			
		} 
		else {
			response.setContentType("text/plain");
			DataResponse dresp = (DataResponse) resp;

			log.debug("QUERY RETURNED " + dresp.getData().toString().length() + " chars");
			
			// take care to count provider's data accessed events
			Set<String> providers = dresp.getProviders();
			updateCountsFor.addAll(LogEvent.fromProviders(providers));
			
			//log to the db (for analysis and reporting)
			LogUtils.log(logEntitiesRepository, updateCountsFor,
					Geoloc.fromIpAddress(clientIpAddress(request)));
			
			writer.write(dresp.getData().toString());
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