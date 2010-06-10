package cpath.webservice.validation.protocol;

import org.apache.log4j.Logger;

import java.util.Set;

/**
 * Validates Client/Browser Request, Version 1.0.
 *
 * @author cPath Dev Team.
 */
class ProtocolValidatorVersion1 {
    private Logger log = Logger.getLogger(ProtocolValidator.class);

    /**
     * Protocol Request.
     */
    private ProtocolRequest request;

    /**
     * Protocol Constants.
     */
    private ProtocolConstantsVersion1 constants = new ProtocolConstantsVersion1();

    /**
     * Constructor.
     *
     * @param request Protocol Request.
     */
    ProtocolValidatorVersion1 (ProtocolRequest request) {
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
            validateEmptySet();
            validateCommand();
            validateVersion();
            validateFormat();
            validateQuery();
        } catch (ProtocolException e) {
            log.info("Protocol Exception:  " + e.getStatusCode()
                + " --> " + e.getMessage());
            throw e;
        }
    }

    /**
     * Validates the Command Parameter.
     *
     * @throws ProtocolException  Indicates Violation of Protocol.
     * @throws NeedsHelpException Indicates user requests/needs help.
     */
	private void validateCommand() throws ProtocolException, NeedsHelpException {
		if (request.getCommand() == null) {
			throw new ProtocolException(ProtocolStatusCode.MISSING_ARGUMENTS,
					"Argument:  '" + ProtocolRequest.ARG_COMMAND
					+ "' is not specified." + ProtocolValidator.HELP_MESSAGE);
		}
		Set<String> set;
		// Special Case
		set = constants.getValidBioPaxCommands();
		if (request.getCommand().equals(
				ProtocolConstants.COMMAND_GET_BY_KEYWORD)) {
			if (request.getFormat() != null
					&& request.getFormat().equals(
							ProtocolConstantsVersion1.FORMAT_BIO_PAX)) {
				throw new ProtocolException(ProtocolStatusCode.BAD_FORMAT,
						"BioPAX format not supported for this command.");
			}
		}
		if (!set.contains(request.getCommand())) {
			throw new ProtocolException(ProtocolStatusCode.BAD_COMMAND,
					"Command:  '" + request.getCommand()
							+ "' is not recognized."
							+ ProtocolValidator.HELP_MESSAGE);
		}
		if (request.getCommand().equals(ProtocolConstants.COMMAND_HELP)) {
			throw new NeedsHelpException();
		}
	}

    /**
     * Validates the Format Parameter.
     *
     * @throws ProtocolException Indicates Violation of Protocol.
     */
    private void validateFormat() throws ProtocolException {
        if (request.getFormat() == null) {
            throw new ProtocolException
                    (ProtocolStatusCode.MISSING_ARGUMENTS,
                            "Argument:  '" + ProtocolRequest.ARG_FORMAT
                                    + "' is not specified." + ProtocolValidator.HELP_MESSAGE);
        }
        Set<String> set = constants.getValidBioPaxFormats();
        if (!set.contains(request.getFormat())) {
            throw new ProtocolException(ProtocolStatusCode.BAD_FORMAT,
                    "Format:  '" + request.getFormat()
                            + "' is not recognized."
                            + ProtocolValidator.HELP_MESSAGE);
        }
    }

    /**
     * Validates the UID Parameter.
     *
     * @throws ProtocolException Indicates Violation of Protocol.
     */
    private void validateQuery() throws ProtocolException {
        String command = request.getCommand();
        String q = request.getQuery();
        String org = request.getOrganism();
        boolean qExists = true;
        boolean organismExists = true;
        boolean errorFlag = false;
        if (q == null || q.length() == 0) {
            qExists = false;
        }
        if (org == null || org.length() == 0) {
            organismExists = false;
        }

        if (command.equals(ProtocolConstants.COMMAND_GET_RECORD_BY_CPATH_ID)) {
            // ProtocolConstants.COMMAND_GET_RECORD_BY_CPATH_ID must have a
            // query parameter.  All other commands must have either a query
            // parameter or an organism paramter.
            if (!qExists) {
                errorFlag = true;
            }

            //  Verify that query parameter is an int/long number.
            try {
                Long.parseLong(q);
            } catch (NumberFormatException e) {
                throw new ProtocolException(ProtocolStatusCode.INVALID_ARGUMENT,
                        "Argument:  '" + ProtocolRequest.ARG_QUERY
                                + "' must be an integer value." + ProtocolValidator.HELP_MESSAGE,
                        "Query parameter must be an integer value. "
                                + "Please try again.");
            }
        } else if (command.equals
                (ProtocolConstantsVersion1.COMMAND_GET_TOP_LEVEL_PATHWAY_LIST)) {
            /*
             * ProtocolConstants.COMMAND_GET_TOP_LEVEL_PATHWAY_LIST can appear
             * without a query parameter or an organism parameter.
             */
            return;
        }
        else {
            if (!qExists && !organismExists) {
                errorFlag = true;
            }
        }

        if (errorFlag) {
            throw new ProtocolException(ProtocolStatusCode.MISSING_ARGUMENTS,
                    "Argument:  '" + ProtocolRequest.ARG_QUERY
                            + "' is not specified." + ProtocolValidator.HELP_MESSAGE,
                    "You did not specify a query term.  Please try again.");
        }
    }

    /**
     * Validates the Version Parameter.
     *
     * @throws ProtocolException Indicates Violation of Protocol.
     */
    private void validateVersion() throws ProtocolException {
        if (request.getVersion() == null) {
            throw new ProtocolException(ProtocolStatusCode.MISSING_ARGUMENTS,
                    "Argument: '" + ProtocolRequest.ARG_VERSION
                            + "' is not specified." + ProtocolValidator.HELP_MESSAGE);
        }
        if (!request.getVersion().equals(ProtocolConstantsVersion1.VERSION_1)) {
            throw new ProtocolException
                    (ProtocolStatusCode.VERSION_NOT_SUPPORTED,
                            "This data service currently only supports "
                                    + "version 1.0." + ProtocolValidator.HELP_MESSAGE);
        }
    }

    /**
     * Checks if no arguments are specified.
     * If none are specified, throws NeedsHelpException.
     *
     * @throws NeedsHelpException Indicates user requests/needs help.
     */
    private void validateEmptySet() throws NeedsHelpException {
        if (request.isEmpty()) {
            throw new NeedsHelpException();
        }
    }
}
