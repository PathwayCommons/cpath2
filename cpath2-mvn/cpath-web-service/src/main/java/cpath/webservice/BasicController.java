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

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

import javax.servlet.http.HttpServletResponse;

import static cpath.service.Status.*;
import cpath.service.Status;
import cpath.service.jaxb.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

/**
 * Basic controller.
 * 
 * @author rodche
 */
public abstract class BasicController {
    private static final Log log = LogFactory.getLog(BasicController.class);
   

	/**
	 * Writes an error response body from the error bean.
	 * @param writer
	 * @param response TODO
	 * @param bindingResult
	 * 
	 * @throws IOException 
	 */
	protected void errorResponse(ErrorResponse error, 
			Writer writer, HttpServletResponse response) throws IOException {
		response.setContentType("application/xml");
		writer.write(Status.marshal(error));
	}
	
	
	protected ErrorResponse errorfromBindingResult(BindingResult bindingResult) {
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
		
		ErrorResponse error = BAD_REQUEST.errorResponse(sb.toString());
		return error;
	}
    
	
	protected void stringResponse(ServiceResponse resp, 
			Writer writer, HttpServletResponse response) throws IOException 
	{
		if(resp instanceof ErrorResponse) {
    		errorResponse((ErrorResponse) resp, writer, response);
		} else if(resp.isEmpty()) { // should not be here (normally, it gets converter to ErrorResponse...)
			log.warn("stringResponse: I got an empty ServiceResponce! " +
				"(must be already converted to the ErrorResponse)");
			errorResponse(NO_RESULTS_FOUND.errorResponse(null), writer, response);
		} else {
			response.setContentType("text/plain");
			DataResponse dresp = (DataResponse) resp;
			if(log.isDebugEnabled())
				log.debug("QUERY RETURNED " + dresp.getData().toString().length() + " chars");
			writer.write(dresp.getData().toString());
		}
	}
		
}