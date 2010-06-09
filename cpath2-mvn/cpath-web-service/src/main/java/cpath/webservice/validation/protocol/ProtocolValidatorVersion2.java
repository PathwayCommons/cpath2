package cpath.webservice.validation.protocol;

import org.mskcc.pathdb.form.WebUIBean;
import org.mskcc.pathdb.model.CPathRecord;
import org.mskcc.pathdb.model.ExternalDatabaseRecord;
import org.mskcc.pathdb.model.ExternalDatabaseSnapshotRecord;
import org.mskcc.pathdb.servlet.CPathUIConfig;
import org.mskcc.pathdb.sql.dao.*;
import org.mskcc.pathdb.util.ExternalDatabaseConstants;
import org.mskcc.pathdb.action.web_api.binary_interaction_mode.ExecuteBinaryInteraction;

import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;

/**
 * Validates Client/Browser Request, Version 1.0.
 *
 * @author cPath Dev Team.
 */
class ProtocolValidatorVersion2 {
    /**
     * Protocol Request.
     */
    private ProtocolRequest request;

    /**
     * Protocol Constants.
     */
    private ProtocolConstantsVersion2 constants = new ProtocolConstantsVersion2();

    /**
     * IdType
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
    ProtocolValidatorVersion2(ProtocolRequest request) {
        this.request = request;
    }

    /**
     * Validates the Request object.
     *
     * @throws ProtocolException  Indicates Violation of Protocol.
     * @throws NeedsHelpException Indicates user requests/needs help.
     */
    public void validate() throws ProtocolException, NeedsHelpException {
        try {
            validateCommand();
            validateVersion();
            validateIdType(ID_Type.INPUT_ID_TYPE);
            validateIdType(ID_Type.OUTPUT_ID_TYPE);
            validateDataSources();
            validateQuery();
            validateOutput();
            validateOrganism();
            validateMisc();
        } catch (DaoException e) {
            throw new ProtocolException(ProtocolStatusCode.INTERNAL_ERROR);
        }
    }

    /**
     * Validates the organism paramter.
     * @throws ProtocolException Indicates violocation of Protocol.
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
    protected void validateCommand() throws ProtocolException,
            NeedsHelpException {
        if (request.getCommand() == null) {
            throw new ProtocolException(ProtocolStatusCode.MISSING_ARGUMENTS,
                    "Argument:  '" + ProtocolRequest.ARG_COMMAND
                            + "' is not specified." + ProtocolValidator.HELP_MESSAGE);
        } else {
            HashSet set = constants.getValidCommands();
            if (!set.contains(request.getCommand())) {
                throw new ProtocolException(ProtocolStatusCode.BAD_COMMAND,
                        "Command:  '" + request.getCommand()
                                + "' is not recognized." + ProtocolValidator.HELP_MESSAGE);
            } else if (request.getCommand().equals(ProtocolConstants.COMMAND_HELP)) {
                throw new NeedsHelpException();
            }
        }
    }

    /**
     * Validates the Query Parameter.
     *
     * @throws ProtocolException Indicates Violation of Protocol.
     */
    protected void validateQuery() throws ProtocolException, DaoException {
        String command = request.getCommand();
        String q = request.getQuery();
        if (q == null || q.length() == 0) {
            throw new ProtocolException(ProtocolStatusCode.MISSING_ARGUMENTS,
                    "Argument:  '" + ProtocolRequest.ARG_QUERY
                            + "' is not specified." + ProtocolValidator.HELP_MESSAGE,
                    "You did not specify a query term.  Please try again.");
        } else {
            if (command != null) {
                if (command.equals (ProtocolConstantsVersion2.COMMAND_GET_PATHWAY_LIST)) {
                    String ids[] = q.split("[\\s]");
                    if (ids.length > ProtocolConstantsVersion2.MAX_NUM_IDS) {
                        throw new ProtocolException(ProtocolStatusCode.INVALID_ARGUMENT,
                                "To prevent overloading of the system, clients are "
                                        + "restricted to a maximum of "
                                        + ProtocolConstantsVersion2.MAX_NUM_IDS
                                        + " IDs at a time.");
                    }
                } else if (command.equals(ProtocolConstantsVersion2.COMMAND_GET_PARENT_SUMMARIES)) {
                    long cpathId = convertQueryToLong(q);
                    checkRecordExists(cpathId, q);
                } else if (command.equals(ProtocolConstants.COMMAND_GET_RECORD_BY_CPATH_ID)) {
                    long cPathIds[] = convertQueryToLongs(q);
                }
            }
        }
    }

    /**
     * Validates the Version Parameter.
     *
     * @throws ProtocolException Indicates Violation of Protocol.
     */
    protected void validateVersion() throws ProtocolException {
        if (request.getVersion() == null) {
            throw new ProtocolException(ProtocolStatusCode.MISSING_ARGUMENTS,
                    "Argument: '" + ProtocolRequest.ARG_VERSION
                            + "' is not specified." + ProtocolValidator.HELP_MESSAGE);
        } else if (!request.getVersion().equals(ProtocolConstantsVersion2.VERSION_2)) {
            throw new ProtocolException
                    (ProtocolStatusCode.VERSION_NOT_SUPPORTED,
                            "The web service API currently only supports "
                                    + "version 2.0." + ProtocolValidator.HELP_MESSAGE);
        }
    }

    protected void validateIdType(ID_Type idType) throws ProtocolException {
        String command = request.getCommand();
        if (command != null &&
                (command.equals(ProtocolConstantsVersion2.COMMAND_GET_PATHWAY_LIST) ||
                 command.equals(ProtocolConstants.COMMAND_GET_RECORD_BY_CPATH_ID) ||
                        command.equals(ProtocolConstantsVersion2.COMMAND_GET_NEIGHBORS))) {
            String type = (idType == ID_Type.INPUT_ID_TYPE) ?
                    request.getInputIDType() : request.getOutputIDType();
            if (type != null) {
                WebUIBean webBean = CPathUIConfig.getWebUIBean();
                ArrayList supportedIdList = webBean.getSupportedIdTypes();
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
                command.equals(ProtocolConstantsVersion2.COMMAND_GET_NEIGHBORS)) {
            String output = request.getOutput();
            if (output == null) {
                output = ProtocolConstantsVersion1.FORMAT_BIO_PAX;
                request.setOutput(ProtocolConstantsVersion1.FORMAT_BIO_PAX);
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

    protected void validateDataSources() throws ProtocolException, DaoException {
        String command = request.getCommand();
        if (command != null &&
                (command.equals(ProtocolConstantsVersion2.COMMAND_GET_PATHWAY_LIST) ||
                        command.equals(ProtocolConstantsVersion2.COMMAND_GET_NEIGHBORS))) {
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

    private ArrayList getMasterTermList
            () throws DaoException {
        DaoExternalDbSnapshot dao = new DaoExternalDbSnapshot();
        ArrayList list = dao.getAllNetworkDatabaseSnapshots();
        ArrayList masterTermList = new ArrayList();
        for (int i = 0; i < list.size(); i++) {
            ExternalDatabaseSnapshotRecord snapshotRecord = (ExternalDatabaseSnapshotRecord)
                    list.get(i);
            String masterTerm = snapshotRecord.getExternalDatabase().getMasterTerm();
            masterTermList.add(masterTerm);
        }
        return masterTermList;
    }

    protected void validateMisc() throws ProtocolException, DaoException {

        String command = request.getCommand();

		// get record by cpath id misc args
		if (command != null && command.equals(ProtocolConstants.COMMAND_GET_RECORD_BY_CPATH_ID)) {
			validateMiscGetRecordByCpathIdArgs();
		}

        // get neighbors misc args
        if (command != null && command.equals(ProtocolConstantsVersion2.COMMAND_GET_NEIGHBORS)) {
            validateMiscGetNeighborArgs();
        }
    }

	private void validateMiscGetRecordByCpathIdArgs() throws ProtocolException {

		// validate binary interaction rule
		validateBinaryInteractionRule();
	}

    private void validateMiscGetNeighborArgs() throws ProtocolException, DaoException {

		// validate binary interaction rule
		validateBinaryInteractionRule();

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
            checkRecordExists(recordID, query);
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
	 * Validates the binary interaction rule argument.
	 */
	private void validateBinaryInteractionRule() throws ProtocolException {

        String[] binaryInteractionRules = request.getBinaryInteractionRules();
        if (binaryInteractionRules != null) {
			// get valid rule types
			List<String> ruleTypes = ExecuteBinaryInteraction.getRuleTypesForDisplay();
			// interate through requested rule(s) and check for validity
			for (String rule : binaryInteractionRules) {
				if (!ruleTypes.contains(rule)) {
						throw new ProtocolException(ProtocolStatusCode.INVALID_ARGUMENT,
													ProtocolRequest.ARG_BINARY_INTERACTION_RULE + ": "
													+ rule + " is not a recognized binary interaction rule.");
				}
			}
		}
	}

    /**
     * Checks that the specified cpathId exists within the database.
     */
    private void checkRecordExists(long cpathId, String query) throws DaoException,
            ProtocolException {
        DaoCPath daoCPath = DaoCPath.getInstance();
        CPathRecord record = daoCPath.getRecordById(cpathId);
        if (record == null) {
            throw new ProtocolException(ProtocolStatusCode.INVALID_ARGUMENT,
                    ProtocolRequest.ARG_QUERY + ": an internal record with id: " +
                            query + " cannot be found.");
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

    /**
     * Checks that the query is an integer value.
     */
    private long[] convertQueryToLongs(String q) throws ProtocolException {
        if (q != null) {
            try {
                String idStrs[] = q.split(",");
                long ids[] = new long[idStrs.length];
                for (int i=0; i< idStrs.length; i++) {
                    ids[i] = Long.parseLong(idStrs[i].trim());
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