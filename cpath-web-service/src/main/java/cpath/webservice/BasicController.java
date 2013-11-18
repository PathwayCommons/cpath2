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
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static cpath.service.Status.*;
import cpath.log.jpa.LogEntitiesRepository;
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
	 */
	protected void errorResponse(Status status, String detailedMsg,
			HttpServletResponse response) throws IOException {
		response.sendError(status.getErrorCode(), 
			status.getErrorMsg() + "; " + detailedMsg);
		
		//TODO log to DB (also send to a mailing list?)
	}
	
	
	protected String errorFromBindingResult(BindingResult bindingResult) {
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
    
	
	protected void stringResponse(ServiceResponse resp, 
			Writer writer, HttpServletResponse response) throws IOException 
	{
		if(resp instanceof ErrorResponse) {
			
			errorResponse(((ErrorResponse) resp).getStatus(), ((ErrorResponse) resp).toString(), response);
			
		} 
		else if(resp.isEmpty()) {
			log.warn("stringResponse: I got an empty ServiceResponce " +
				"(must be already converted to the ErrorResponse)");
			
			errorResponse(NO_RESULTS_FOUND, "no results found", response);
			
		} 
		else {
			response.setContentType("text/plain");
			DataResponse dresp = (DataResponse) resp;

			log.debug("QUERY RETURNED " + dresp.getData().toString().length() + " chars");
			
			Set<String> providers = dresp.getProviders();
			
			//TODO success - log to db
			
			writer.write(dresp.getData().toString());
		}
	}
	
	public BufferedImage scaleImage(BufferedImage img, int width, int height,
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
	 * @param request
	 * @param params query arguments to replace request.getQueryString() value, which is empty for POST requests
	 */
	void log(HttpServletRequest request, Object... params) {
		String ip = clientIpAddress(request);
		
		log.info("REQUEST " + ip
			+ "\t" + request.getMethod() 
			+ "\t" + request.getRequestURI()
			+ "\t" + ((params.length == 0) ? request.getQueryString() : Arrays.toString(params))
		);
	}

	
	//TODO add log to the DB method
	
	
	/**
	 * Extracts the client's IP from the request headers.
	 * 
	 * @param request
	 * @return
	 */
	public static String clientIpAddress(HttpServletRequest request) {
		
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