
package cpath.webservice.validation;

import java.net.URI;
import java.util.*;

import cpath.webservice.args.Cmd;
import cpath.webservice.args.OutputFormat;
import cpath.webservice.args.ProtocolVersion;
import cpath.webservice.args.binding.CmdEditor;

/**
 * Validates Client/Browser Request.
 *
 * TODO complete code re-factoring...
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
     * IdType parameter names (keys)
     */
    public enum ID_Type {
        INPUT_ID_TYPE,
        OUTPUT_ID_TYPE
    }
    
    
    /**
     * Constructor.
     *
     * @param request Protocol Request.
     */
    public ProtocolValidator(ProtocolRequest request) {
        this.request = request;
    }

    public void validateVersion() throws ProtocolException {
    	String version = request.getVersion();
        if(ProtocolVersion.fromValue(version) == null) {
        	throw new IllegalArgumentException ("Unsupported version # specified:  "
                    + version);
        }
    }
    
    /**
     * Validates the Request object.
     *
     * @throws ProtocolException  Indicates Violation of Protocol.
     */
	public void validate() throws ProtocolException {
		validateVersion();
		validateCommand();
		validateIdType(ID_Type.INPUT_ID_TYPE);
		validateIdType(ID_Type.OUTPUT_ID_TYPE);
		validateDataSources();
		validateQuery();
		validateOutput();
		validateOrganism();
		validateMisc();
	}

	
    /**
     * Validates the organism parameter.
     * @throws ProtocolException Indicates violation of Protocol.
     */
    protected void validateOrganism() throws ProtocolException {
        String organism = request.getOrganism();
        if (organism != null && organism.length() > 0) {
            try {
                Integer temp = new Integer(organism);
            } catch (NumberFormatException e) {
                throw new ProtocolException(ProtocolStatusCode.INVALID_ARGUMENT,
                        "Argument:  '" + ProtocolRequest.ARG_ORGANISM
                        + "' must must be an integer value, e.g. 9606.");
            }
        }
    }

    /**
     * Validates the Command Parameter.
     *
     * @throws ProtocolException  Indicates Violation of Protocol.
     * @throws NeedsHelpException Indicates user requests/needs help.
     */
    protected void validateCommand() throws ProtocolException 
    {
        if (request.getCommand() == null) {
            throw new ProtocolException(ProtocolStatusCode.MISSING_ARGUMENTS,
                    "Argument:  '" + ProtocolRequest.ARG_COMMAND
                            + "' is not specified." + ProtocolValidator.HELP_MESSAGE);
        } else {
        	CmdEditor cmdEditor = new CmdEditor();
        	cmdEditor.setAsText(request.getCommand());
        	Cmd value = (Cmd) cmdEditor.getValue();
            if(value == null) {
                throw new ProtocolException(ProtocolStatusCode.BAD_COMMAND,
                        "Command:  '" + request.getCommand()
                                + "' is not recognized." + ProtocolValidator.HELP_MESSAGE);
            } 
        }
    }

    /**
     * Validates the Query Parameter.
     *
     * @throws ProtocolException Indicates Violation of Protocol.
     */
    protected void validateQuery() throws ProtocolException {
        String command = request.getCommand();
        String q = request.getQuery();
        if (q == null || q.length() == 0) {
            throw new ProtocolException(ProtocolStatusCode.MISSING_ARGUMENTS,
                    "Argument:  '" + ProtocolRequest.ARG_QUERY
                            + "' is not specified." + ProtocolValidator.HELP_MESSAGE,
                    "You did not specify a query term.  Please try again.");
        } else {
            if (command != null) {
            	if (command.equalsIgnoreCase(Cmd.GET_RECORD_BY_CPATH_ID.name())) {
                    queryToIdList(q); // checks IDs
                }
            }
        }
    }
    

    protected void validateIdType(ID_Type idType) throws ProtocolException {
        String command = request.getCommand();
        if(command == null) 
        	throw new ProtocolException(ProtocolStatusCode.BAD_COMMAND,
                    idType.name() + " must be set!");
        
        if (command.equalsIgnoreCase(Cmd.GET_PATHWAYS.name()) ||
            command.equalsIgnoreCase(Cmd.GET_RECORD_BY_CPATH_ID.name()) ||
            command.equalsIgnoreCase(Cmd.GET_NEIGHBORS.name()))
        {
            // get input_id_type or output_id_type parameter string value
        	String type = (idType == ID_Type.INPUT_ID_TYPE) ?
                    request.getInputIDType() : request.getOutputIDType();
            if (type != null) {
            	Set<String> supportedIdList = null; // TODO get the list of valid ID types from BioDataTypes...
                if (!supportedIdList.contains(type)) {
                    StringBuffer buf = new StringBuffer();
                    for (int i = 0; i < supportedIdList.size(); i++) {
                        buf.append(supportedIdList.get(i));
                        if (i < supportedIdList.size() - 1) {
                            buf.append(", ");
                        }
                    }
                    String idTypeStr = (idType == ID_Type.INPUT_ID_TYPE) ?
                            ProtocolRequest.ARG_INPUT_ID_TYPE : ProtocolRequest.ARG_OUTPUT_ID_TYPE;
                    throw new ProtocolException(ProtocolStatusCode.INVALID_ARGUMENT,
                            idTypeStr +
                                    " must be set to one of the following: " +
                                    buf.toString() + ".");
                }
            }
        }
    }

    protected void validateOutput() throws ProtocolException {
        String command = request.getCommand();

        if (command != null &&
                command.equals(Cmd.GET_PATHWAYS.name())) {
            String output = request.getOutput();
            if (output == null) {
                output = OutputFormat.BIOPAX.name().toLowerCase();
                request.setOutput(output);
            }
            if (output != null &&
                    !output.equalsIgnoreCase(ProtocolConstantsVersion1.FORMAT_BIO_PAX) &&
                    !output.equalsIgnoreCase(ProtocolConstantsVersion2.FORMAT_ID_LIST) &&
				    !output.equalsIgnoreCase(ProtocolConstantsVersion2.FORMAT_BINARY_SIF) &&
				    !output.equalsIgnoreCase(ProtocolConstantsVersion2.FORMAT_IMAGE_MAP) &&
				    !output.equalsIgnoreCase(ProtocolConstantsVersion2.FORMAT_IMAGE_MAP_THUMBNAIL) &&
				    !output.equalsIgnoreCase(ProtocolConstantsVersion2.FORMAT_IMAGE_MAP_IPHONE) &&
				    !output.equalsIgnoreCase(ProtocolConstantsVersion2.FORMAT_IMAGE_MAP_FRAMESET)) {
                throw new ProtocolException(ProtocolStatusCode.INVALID_ARGUMENT,
                        ProtocolRequest.ARG_OUTPUT +
                                " must be set to one of the following: " +
                                ProtocolConstantsVersion1.FORMAT_BIO_PAX + " " +
                                ProtocolConstantsVersion2.FORMAT_ID_LIST + "." +
                                ProtocolConstantsVersion2.FORMAT_BINARY_SIF + "." + 
                                ProtocolConstantsVersion2.FORMAT_IMAGE_MAP + "." +
                                ProtocolConstantsVersion2.FORMAT_IMAGE_MAP_THUMBNAIL + "." +
                                ProtocolConstantsVersion2.FORMAT_IMAGE_MAP_FRAMESET + ".");
            }
        }
        if (command != null &&
                command.equals(ProtocolConstants.COMMAND_GET_RECORD_BY_CPATH_ID)) {
            String output = request.getOutput();
            if (output == null || output.trim().length() == 0) {
                throw new ProtocolException(ProtocolStatusCode.MISSING_ARGUMENTS,
                        "Argument:  '" + ProtocolRequest.ARG_OUTPUT
                                + "' is not specified." + ProtocolValidator.HELP_MESSAGE,
                        "You did not specify an output format.  Please try again.");
            } else {
                boolean validFormat = false;
                if (output.equals(ProtocolConstantsVersion1.FORMAT_BIO_PAX)) {
                    validFormat = true;
                } else if (output.equals(ProtocolConstantsVersion2.FORMAT_BINARY_SIF)) {
                    validFormat = true;
                } else if (output.equals(ProtocolConstantsVersion2.FORMAT_GSEA)) {
                    validFormat = true;
                } else if (output.equals(ProtocolConstantsVersion2.FORMAT_PC_GENE_SET)) {
                    validFormat = true;
                }
                if (validFormat == false) {
                    throw new ProtocolException(ProtocolStatusCode.INVALID_ARGUMENT,
                            ProtocolRequest.ARG_OUTPUT +
                                    " must be set to: " +
                                    ProtocolConstantsVersion1.FORMAT_BIO_PAX + ", " +
                                    ProtocolConstantsVersion2.FORMAT_BINARY_SIF + ", " +
                                    ProtocolConstantsVersion2.FORMAT_GSEA + ", or " +
                                    ProtocolConstantsVersion2.FORMAT_PC_GENE_SET + ".");
                }
            }
        }
    }

    protected void validateDataSources() throws ProtocolException {
        String command = request.getCommand();
        if (command != null &&
                (command.equalsIgnoreCase(Cmd.GET_PATHWAYS.name()) ||
                 command.equalsIgnoreCase(Cmd.GET_NEIGHBORS.name()))) 
        {
            String dataSources[] = request.getDataSources();
            if (dataSources != null) {
                ArrayList masterTermList = getMasterTermList();
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
        String command = request.getCommand();

		// get record by cpath id misc args
		if (command != null && command.equals(Cmd.GET_RECORD_BY_CPATH_ID.name())) {
			validateMiscGetRecordByCpathIdArgs();
		}

        // get neighbors misc args
        if (command != null && command.equals(Cmd.GET_NEIGHBORS.name())) {
            validateMiscGetNeighborArgs();
        }
    }


    private void validateMiscGetNeighborArgs() throws ProtocolException{

        // validate fully connected
        String fullyConnected = request.getFullyConnected();
        if (fullyConnected != null &&
                !fullyConnected.equalsIgnoreCase("yes") &&
                !fullyConnected.equalsIgnoreCase("no")) {
            throw new ProtocolException(ProtocolStatusCode.INVALID_ARGUMENT,
                    ProtocolRequest.ARG_FULLY_CONNECTED +
                            " must be set to one of the following: yes no.");
        }

        // validate query
        String inputIDTerm = request.getInputIDType();
        String query = request.getQuery();
        if (inputIDTerm == null ||
                inputIDTerm.equals(ExternalDatabaseConstants.INTERNAL_DATABASE)) {
            long recordID = convertQueryToLong(query);
            assertIdIsURI(recordID, query);
        } else if (inputIDTerm != null &&
                !inputIDTerm.equals(ExternalDatabaseConstants.INTERNAL_DATABASE)) {
            DaoExternalDb daoExternalDb = new DaoExternalDb();
            DaoExternalLink daoExternalLinker = DaoExternalLink.getInstance();
            ExternalDatabaseRecord dbRecord = daoExternalDb.getRecordByTerm(inputIDTerm);
            ArrayList externalLinkRecords =
                    daoExternalLinker.getRecordByDbAndLinkedToId(dbRecord.getId(),
                            query);
            if (externalLinkRecords == null || externalLinkRecords.size() == 0) {
                throw new ProtocolException(ProtocolStatusCode.INVALID_ARGUMENT,
                        ProtocolRequest.ARG_QUERY + ": " +
                                " an internal record with the " + inputIDTerm +
                                " external database id: " + query +
                                " cannot be found.");
            }
        }
    }


    /**
     * Checks that the specified Id is URI.
     */
    private void assertIdIsURI(String id, String query) throws ProtocolException {
    	try {
			URI.create(id);
		} catch (IllegalArgumentException e) {
			 throw new ProtocolException(ProtocolStatusCode.INVALID_ARGUMENT,
	                    ProtocolRequest.ARG_QUERY + ": " +
	                    query + " contains invalid ID (not URI): " + id);
		}	
    }

    /**
     * Checks that the query is an integer value.
     */
    private long convertQueryToLong(String q) throws ProtocolException {
        if (q != null) {
            try {
                return Long.parseLong(q);
            } catch (NumberFormatException e) {
                throw new ProtocolException(ProtocolStatusCode.INVALID_ARGUMENT,
                        "q must be an integer value.");
            }
        }
        return -1;
    }


    private String[] queryToIdList(String q) throws ProtocolException {
        if (q != null) {
            try {
                String idStrs[] = q.split(",");
                String ids[] = new String[idStrs.length];
                for (int i=0; i< idStrs.length; i++) {
                    ids[i] = idStrs[i].trim();
                }
                return ids;
            } catch (NumberFormatException e) {
                throw new ProtocolException(ProtocolStatusCode.INVALID_ARGUMENT,
                        "q must contain integer values only.");
            }
        }
        return null;
    }
    
}