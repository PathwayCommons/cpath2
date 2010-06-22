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

import java.net.URI;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cpath.dao.CPathService.OutputFormat;
import cpath.warehouse.internal.BioDataTypes;
import cpath.warehouse.internal.BioDataTypes.Type;
import cpath.webservice.args.*;

/**
 * Additionally Validates Webservice Client's Request.
 */
public class ProtocolValidator {
	private static final Log log = LogFactory.getLog(ProtocolValidator.class);
    public static final String HELP_MESSAGE = "  Please try again.";
    
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
     *
     * @throws ProtocolException  Indicates Violation of Protocol.
     */
	public void validate() throws ProtocolException {
		validateVersion();
		validateCommand();
		validateIdType();
		validateDataSources();
		validateQuery();
		validateOutput();
		validateMisc();
	}

	
    public void validateVersion() throws ProtocolException {
        if(request.getVersion() == null) {
        	request.setVersion(ProtocolVersion.VERSION_3); // do not make it error, just use default...
        }
    }

    
    /**
     * Validates the Command Parameter.
     *
     * @throws ProtocolException  Indicates Violation of Protocol.
     */
    protected void validateCommand() throws ProtocolException 
    {
        if (request.getCommand() == null) {
            throw new ProtocolException(ProtocolStatusCode.MISSING_ARGUMENTS,
                    "Argument:  '" + ProtocolRequest.ARG_COMMAND
                            + "' is not specified." + ProtocolValidator.HELP_MESSAGE);
        }
    }

    /**
     * Validates the Query Parameter.
     *
     * @throws ProtocolException Indicates Violation of Protocol.
     */
    protected void validateQuery() throws ProtocolException {
        Cmd command = request.getCommand();
        String q = request.getQuery();
        if (q == null || q.length() == 0) {
            throw new ProtocolException(ProtocolStatusCode.MISSING_ARGUMENTS,
                    "Argument:  '" + ProtocolRequest.ARG_QUERY
                            + "' is not specified." + ProtocolValidator.HELP_MESSAGE,
                    "You did not specify a query term.  Please try again.");
        } else {
            if (command == Cmd.GET_RECORD_BY_CPATH_ID) {
            	queryToIdList(q); // checks IDs
            }
        }
    }

    
	protected void validateIdType() throws ProtocolException {
		Cmd command = request.getCommand();
		if (command == Cmd.GET_PATHWAYS
				|| command == Cmd.GET_RECORD_BY_CPATH_ID
				|| command == Cmd.GET_NEIGHBORS) {
			// get input_id_type or output_id_type parameter string value
			for (String type : new String[] { request.getInputIDType(),
					request.getOutputIDType() }) {
				if (type != null) {
					Set<String> supportedIdList = BioDataTypes
							.getDataSourceKeys(Type.IDENTIFIER);
					if (!supportedIdList.contains(type.toLowerCase())) {
						StringBuffer buf = new StringBuffer();
						int i = 0;
						for (String s : supportedIdList) {
							buf.append(s);
							if (++i < supportedIdList.size()) {
								buf.append(", ");
							}
						}
						throw new ProtocolException(
							ProtocolStatusCode.INVALID_ARGUMENT,
							"'*_id_type' must be one of: " + buf.toString());
					}
				}
			}
		}
	}

	
    protected void validateOutput() throws ProtocolException {
        Cmd command = request.getCommand();
        OutputFormat output = request.getOutput();
        if (output == null) {
        	output = OutputFormat.BIOPAX;
            request.setOutput(output);
            if(log.isWarnEnabled())
            	log.warn("Output format was missing; set default (biopax)");
        }
        
        if (command == Cmd.GET_RECORD_BY_CPATH_ID) {
                if (output != OutputFormat.BIOPAX
                	&& output != OutputFormat.BINARY_SIF
                	&& output != OutputFormat.GSEA
                	&& output != OutputFormat.PC_GENE_SET) 
                {
                    throw new ProtocolException(ProtocolStatusCode.INVALID_ARGUMENT,
                            ProtocolRequest.ARG_OUTPUT +
                                    " must be set to: " +
                                    OutputFormat.BIOPAX + ", " +
                                    OutputFormat.BINARY_SIF + ", " +
                                    OutputFormat.GSEA + ", or " +
                                    OutputFormat.PC_GENE_SET + ".");
                }
            }
    }

    
    protected void validateDataSources() throws ProtocolException {
        Cmd command = request.getCommand();
        if (command == Cmd.GET_PATHWAYS || command == Cmd.GET_NEIGHBORS) 
        {
            String dataSources[] = request.getDataSources();
            if (dataSources != null) {
                Set<String> masterTermList = BioDataTypes.getDataSourceKeys(Type.PATHWAY_DATA);
                for (int i = 0; i < dataSources.length; i++) {
                    if (!masterTermList.contains(dataSources[i])) {
                        throw new ProtocolException(ProtocolStatusCode.INVALID_ARGUMENT,
                                ProtocolRequest.ARG_DATA_SOURCE + ": "
                                        + dataSources[i] + " is not a recognized data source.");
                    }
                }
            }
        }
    }


    protected void validateMisc() throws ProtocolException {
        Cmd command = request.getCommand();

		// get record by cpath id misc args
		if (command == Cmd.GET_RECORD_BY_CPATH_ID) {
			// validate binary interaction rule
			validateBinaryInteractionRule();
		}

        // get neighbors misc args
        if (command == Cmd.GET_NEIGHBORS) {
            validateMiscGetNeighborArgs();
        }
    }


    private void validateMiscGetNeighborArgs() throws ProtocolException{

		// validate binary interaction rule
		validateBinaryInteractionRule();
    	
        // validate fully connected 
    	//TODO remove 'fullyConnected' if it's not used at all
        String fullyConnected = request.getFullyConnected();
        if (fullyConnected != null &&
            !fullyConnected.equalsIgnoreCase("yes") &&
            !fullyConnected.equalsIgnoreCase("no")) 
        {
            throw new ProtocolException(ProtocolStatusCode.INVALID_ARGUMENT,
            	ProtocolRequest.ARG_FULLY_CONNECTED +
                	" must be set to one of the following: yes no.");
        }

        // validate query
        String inputIDTerm = request.getInputIDType();
        String query = request.getQuery();
		if (inputIDTerm == null) {
			assertIdIsURI(query); // one URI (rdf id) expected
		} else if (!BioDataTypes.getDataSourceKeys(Type.IDENTIFIER)
				.contains(inputIDTerm)) {
			throw new ProtocolException(ProtocolStatusCode.INVALID_ARGUMENT,
				ProtocolRequest.ARG_QUERY + ": " + inputIDTerm
				+ " database id: " + query + " is not supported " +
				"by this query syntax (try cmd=search instead).");
		}
    }


    /**
     * Checks that the specified Id is URI.
     */
    private void assertIdIsURI(String id) throws ProtocolException {
    	try {
			URI.create(id);
		} catch (IllegalArgumentException e) {
			 throw new ProtocolException(ProtocolStatusCode.INVALID_ARGUMENT,
	                    ProtocolRequest.ARG_QUERY + " contains invalid ID (not URI): "
	                    + id);
		}	
    }


	private String[] queryToIdList(String q) throws ProtocolException {
		if (q != null) {
			String idStrs[] = q.split(",");
			String ids[] = new String[idStrs.length];
			for (int i = 0; i < idStrs.length; i++) {
				ids[i] = idStrs[i].trim();
				assertIdIsURI(ids[i]);
			}
			return ids;
		}
		return null;
	}
    
	
	/**
	 * Validates the binary interaction rule argument.
	 */
	private void validateBinaryInteractionRule() throws ProtocolException {
        String[] binaryInteractionRules = request.getBinaryInteractionRules();
        if (binaryInteractionRules != null) {
			// get valid rule types (incl. old names)
			Collection<String> ruleTypes = BinaryInteractionRule.allNames();
			// interate through requested rule(s) and check for validity
			for (String rule : binaryInteractionRules) {
				if (!ruleTypes.contains(rule.toUpperCase())) {
						throw new ProtocolException(ProtocolStatusCode.INVALID_ARGUMENT,
													ProtocolRequest.ARG_BINARY_INTERACTION_RULE + ": "
													+ rule + " is not a recognized binary interaction rule.");
				}
			}
		}
	}
}