// $Id: ProtocolValidator.java,v 1.23 2008/11/26 20:22:27 grossben Exp $
//------------------------------------------------------------------------------
/** Copyright (c) 2006 Memorial Sloan-Kettering Cancer Center.
 **
 ** Code written by: Ethan Cerami
 ** Authors: Ethan Cerami, Gary Bader, Chris Sander
 **
 ** This library is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** any later version.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** Memorial Sloan-Kettering Cancer Center 
 ** has no obligations to provide maintenance, support,
 ** updates, enhancements or modifications.  In no event shall
 ** Memorial Sloan-Kettering Cancer Center
 ** be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** Memorial Sloan-Kettering Cancer Center 
 ** has been advised of the possibility of such damage.  See
 ** the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this library; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 **/
package cpath.webservice.validation.protocol;

/**
 * Validates Client/Browser Request.
 *
 * @author Ethan Cerami
 */
public class ProtocolValidator {
    /**
     * Help Message
     */
    public static final String HELP_MESSAGE = "  Please try again.";

    /**
     * Protocol Request.
     */
    private ProtocolRequest request;

    /**
     * Constructor.
     *
     * @param request Protocol Request.
     */
    public ProtocolValidator(ProtocolRequest request) {
        this.request = request;
    }

    /**
     * Validates the Request object.
     * @param version Must be ProtocolConstants.VERSION_1 or ProtocolConstants.VERSION_2.
     *
     * @throws ProtocolException  Indicates Violation of Protocol.
     * @throws NeedsHelpException Indicates user requests/needs help.
     */
    public void validate(String version) throws ProtocolException, NeedsHelpException {
        try {
            if (version.equals(ProtocolConstantsVersion1.VERSION_1)) {
                ProtocolValidatorVersion1 validator1 = new ProtocolValidatorVersion1(request);
                validator1.validate();
            } else if (version.equals(ProtocolConstantsVersion2.VERSION_2)) {
                ProtocolValidatorVersion2 validator2 = new ProtocolValidatorVersion2(request);
                validator2.validate();
            } else if (version.equals(ProtocolConstantsVersion3.VERSION_3)) {
                ProtocolValidatorVersion3 validator3 = new ProtocolValidatorVersion3(request);
                validator3.validate();
            } else {
                throw new IllegalArgumentException ("Unsupported version # specified:  "
                    + version);
            }
        } catch (ProtocolException e) {
            throw e;
        }
    }
}