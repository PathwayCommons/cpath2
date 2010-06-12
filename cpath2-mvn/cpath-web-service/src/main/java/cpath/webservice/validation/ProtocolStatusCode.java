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

package cpath.webservice.validation;

import java.io.StringWriter;
import java.math.BigInteger;
import java.util.ArrayList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import cpath.webservice.jaxb.ErrorType;

/**
 * Enumeration of Protocol Status Codes.
 *
 * @author rodche
 */
public enum ProtocolStatusCode {
    /**
     * Status Code:  OK.
     */
    OK(200, "OK, data follows"),

    /**
     * Status Code:  Bad Command.
     */
    BAD_COMMAND(450, "Bad Command (command not recognized)"),

    /**
     * Status Code:  Bad Format.
     */
    BAD_FORMAT(451, "Bad Data Format (data format not recognized)"),

    /**
     * Status Code:  Bad Request, Missing Arguments.
     */
    MISSING_ARGUMENTS(452, "Bad Request (missing arguments)"),

    /**
     * Status Code:  Bad Request, Invalid Argument.
     */
    INVALID_ARGUMENT(453, "Bad Request (invalid arguments)"),

    /**
     * Status Code:  No Results Found.
     */
    NO_RESULTS_FOUND(460, "No Results Found"),

    /**
     * Status Code:  Version Not Supported.
     */
    VERSION_NOT_SUPPORTED(470, "Version not supported"),

    /**
     * Status Code:  Internal Server Error.
     */
    INTERNAL_ERROR(500, "Internal Server Error");

    /*
     * ErrorType xml bean
     */
    private final ErrorType errorType;
    
    private static final JAXBContext jaxbContext;
    
	static {
		try {
			jaxbContext = JAXBContext.newInstance("cpath.webservice.jaxb");
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}
    
    
    /**
     * Gets Error Code.
     *
     * @return Error Code.
     */
    public int getErrorCode() {
        return errorType.getErrorCode().intValue();
    }

    /**
     * Gets Error Message.
     *
     * @return Error Message.
     */
    public String getErrorMsg() {
        return errorType.getErrorMsg();
    }

    /**
     * Gets Complete List of all Status Codes.
     *
     * @return ArrayList of ProtocolStatusCode Objects.
     */
    public static ArrayList<String> getAllStatusCodes() {
        ArrayList<String> list = new ArrayList<String>();
        for(ProtocolStatusCode statusCode : ProtocolStatusCode.values()) {
        	list.add(statusCode.name());
        }
        return list;
    }

    /**
     * Private Constructor.
     *
     * @param errorCode Error Code.
     */
    private ProtocolStatusCode(int errorCode, String msg) {
		errorType = new ErrorType();
		errorType.setErrorCode(BigInteger.valueOf(errorCode));
		errorType.setErrorMsg(msg);
    }
	
	public ProtocolStatusCode fromCode(int code) {
		for(ProtocolStatusCode type : ProtocolStatusCode.values()) {
			if(type.getErrorCode() == code)
				return type;
		}
		return null;
	}
	
	public ErrorType getError() {
		ErrorType e = new ErrorType();
		e.setErrorCode(errorType.getErrorCode());
		e.setErrorDetails(errorType.getErrorDetails());
		e.setErrorMsg(errorType.getErrorMsg());
		return e;
	}
	
	public static String marshal(ErrorType error) {
		StringWriter writer = new StringWriter();
		try {
			Marshaller ma = jaxbContext.createMarshaller();
			ma.setProperty("jaxb.formatted.output", true);
			ma.marshal(
			new JAXBElement<ErrorType>(new QName("","error"), ErrorType.class, error), 
			writer);
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
		return writer.toString();
	}

}
