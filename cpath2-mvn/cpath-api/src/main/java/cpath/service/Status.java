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

package cpath.service;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;

import javax.xml.bind.JAXBContext;
//import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
//import javax.xml.namespace.QName;

import cpath.service.jaxb.ErrorResponse;

/**
 * Enumeration of Protocol Status Codes.
 *
 * @author rodche
 */
public enum Status {
    /**
     * Status Code:  OK.
     */
    OK(200, "OK, data follows"), //TODO remove if not used anymore...

    /**
     * Status Code:  Bad Command.
     */
    BAD_COMMAND(450, "Bad Command (command not recognized)"),

    /**
     * Status Code:  Bad Request, Missing Arguments.
     */
    BAD_REQUEST(452, "Bad Request (missing/illegal arguments)"),

    /**
     * Status Code:  No Results Found.
     */
    NO_RESULTS_FOUND(460, "No Results Found"),


    /**
     * Status Code:  Internal Server Error.
     */
    INTERNAL_ERROR(500, "Internal Server Error");


    private final Integer errorCode;
    private final String errorMsg;
    
    
    private static final JAXBContext jaxbContext;
    
	static {
		try {
			jaxbContext = JAXBContext.newInstance(ErrorResponse.class);//, SearchHit.class, SearchResponse.class);
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}
    

	/**
	 * Private Constructor.
	 * 
	 * @param errorCode
	 * @param msg
	 */
    private Status(int errorCode, String msg) {
		this.errorCode = Integer.valueOf(errorCode);
		this.errorMsg = msg;
    }    
	
	
    /**
     * Gets Error Code.
     *
     * @return Error Code.
     */
    public int getErrorCode() {
        return errorCode.intValue();
    }

    /**
     * Gets Error Message.
     *
     * @return Error Message.
     */
    public String getErrorMsg() {
        return errorMsg;
    }

    /**
     * Gets Complete List of all Status Codes.
     *
     * @return ArrayList of Status Objects.
     */
    public static ArrayList<String> getAllStatusCodes() {
        ArrayList<String> list = new ArrayList<String>();
        for(Status statusCode : Status.values()) {
        	list.add(statusCode.name());
        }
        return list;
    }

	public static Status fromCode(int code) {
		for(Status type : Status.values()) {
			if(type.getErrorCode() == code)
				return type;
		}
		return null;
	}
	
	
	public static String marshal(ErrorResponse error) {
		StringWriter writer = new StringWriter();
		try {
			Marshaller ma = jaxbContext.createMarshaller();
			ma.setProperty("jaxb.formatted.output", true);
//			ma.marshal(new JAXBElement<ErrorResponse>(new QName("","errorResponse"), ErrorResponse.class, error), writer);
			ma.marshal(error, writer);
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
		return writer.toString();
	}

	
	/**
	 * Gets the error as XML string.
	 * 
	 * @param str error details (exception or string)
	 * @return XML error message
	 */
	public String errorResponseXml(Object str) {
		ErrorResponse errorResponse = errorResponse(str);
		return marshal(errorResponse);
	}
	
    
	/**
	 * Creates a desired error bean from an object
	 * (e.g., exception or string)
	 * 
	 * @param o
	 * @return
	 */
	public ErrorResponse errorResponse(Object o) 
    {
		ErrorResponse error = new ErrorResponse();
		error.setErrorCode(errorCode);
		error.setErrorMsg(errorMsg);
		
		if(o instanceof Exception) {
			error.setErrorDetails(o.toString() + "; " 
				+ Arrays.toString(((Exception)o).getStackTrace()));
		} else {
			if(o != null)
				error.setErrorDetails(o.toString());
		}
		
		return error;
    } 

}
