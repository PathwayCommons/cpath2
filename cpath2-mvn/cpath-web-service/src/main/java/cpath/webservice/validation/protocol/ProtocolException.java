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

package cpath.webservice.validation.protocol;

import java.io.*;
import java.util.Stack;

import cpath.webservice.jaxb.ErrorType;

/**
 * Encapsulates a Violation of the Data Service Protocol.
 *
 * @author cerami
 * @author rodche - 2010, re-factored for cPathSquared
 */
public class ProtocolException extends Exception {
	private static final long serialVersionUID = 1L;

	/**
     * Status Code Object.
     */
    private ProtocolStatusCode statusCode;

    /**
     * Error Message Details.
     */
    private String xmlErrorMessage;

    /**
     * Error Message for Displaying to End User of Web Interface.
     */
    private String webErrorMessage;

    /**
     * Constructor.
     *
     * @param statusCode Protocol Status Code.
     */
    public ProtocolException(ProtocolStatusCode statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Constructor.
     *
     * @param statusCode Protocol Status Code.
     * @param details    Error Message Details.
     */
    public ProtocolException(ProtocolStatusCode statusCode, String details) {
        this.statusCode = statusCode;
        this.xmlErrorMessage = details;
    }

    /**
     * Constructor.
     *
     * @param statusCode      Protocol Status Code.
     * @param xmlErrorMessage Error Message Details (for xml user)
     * @param webErrorMessage Error Message Details (for web user)
     */
    public ProtocolException(ProtocolStatusCode statusCode,
            String xmlErrorMessage, String webErrorMessage) {
        this.statusCode = statusCode;
        this.xmlErrorMessage = xmlErrorMessage;
        this.webErrorMessage = webErrorMessage;
    }

    /**
     * Constructor.
     *
     * @param statusCode Protocol Status Code.
     * @param e          Root Exception.
     */
    public ProtocolException(ProtocolStatusCode statusCode, Exception e) {
        super(e);
        this.statusCode = statusCode;
    }

    /**
     * Constructor.
     *
     * @param statusCode Protocol Status Code.
     * @param e          Root Exception.
     */
    public ProtocolException(ProtocolStatusCode statusCode, Throwable e) {
        super(e);
        this.statusCode = statusCode;
    }

    /**
     * Gets the Status Code.
     *
     * @return Status Code.
     */
    public ProtocolStatusCode getStatusCode() {
        return statusCode;
    }

    /**
     * Gets XML Error Message
     *
     * @return Error Details String.
     */
    public String getXmlErrorMessage() {
        return this.xmlErrorMessage;
    }

    /**
     * Gets Web Error Message
     *
     * @return Error Details String.
     */
    public String getWebErrorMessage() {
        if (webErrorMessage != null) {
            return webErrorMessage;
        } else {
            return this.xmlErrorMessage;
        }
    }

    /**
     * Gets Error Message.
     *
     * @return Error Message.
     */
    public String getMessage() {
        return new String(statusCode.getErrorCode() + ":  "
                + statusCode.getErrorMsg() + ": " + this.xmlErrorMessage);
    }

    /**
     * Gets XML Representation of Error.
     *
     * @return XML String containing Error Message.
     */
    public String toXml() {
    	// get the xml schema's ErrorType bean
    	ErrorType errorType = statusCode.getError();
        Throwable rootCause = getRootCause(this);
        if (xmlErrorMessage != null) {
        	errorType.setErrorDetails(xmlErrorMessage);
        } else if (rootCause != null) {
            StringWriter writer = new StringWriter();
            PrintWriter pwriter = new PrintWriter(writer);
            rootCause.printStackTrace(pwriter);
            errorType.setErrorDetails(writer.toString());
        }
        return ProtocolStatusCode.marshal(errorType);
    }

    /**
     * Gets the Root Cause of this Exception.
     */
    private Throwable getRootCause(Throwable throwable) {
        Stack stack = new Stack();
        stack.push(throwable);
        try {
            Throwable temp = throwable.getCause();
            while (temp != null) {
                stack.push(temp);
                temp = temp.getCause();
            }
            return (Throwable) stack.pop();
        } catch (NullPointerException e) {
            return throwable;
        }
    }
}
