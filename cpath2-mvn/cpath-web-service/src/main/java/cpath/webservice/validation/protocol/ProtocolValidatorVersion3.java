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
 * Validates Client/Browser Request, Version 3.0.
 *
 * @author cPath Dev Team.
 */
class ProtocolValidatorVersion3 extends ProtocolValidatorVersion2 {
    /**
     * Protocol Request.
     */
    private ProtocolRequest request;

    /**
     * Constructor.
     *
     * @param request Protocol Request.
     */
    ProtocolValidatorVersion3(ProtocolRequest request) {
		super(request);
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
            validateIdType(ProtocolValidatorVersion2.ID_Type.INPUT_ID_TYPE);
            validateIdType(ProtocolValidatorVersion2.ID_Type.OUTPUT_ID_TYPE);
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
     * Validates the Version Parameter.
     *
     * @throws ProtocolException Indicates Violation of Protocol.
     */
    protected void validateVersion() throws ProtocolException {
        if (request.getVersion() == null) {
            throw new ProtocolException(ProtocolStatusCode.MISSING_ARGUMENTS,
                    "Argument: '" + ProtocolRequest.ARG_VERSION
                            + "' is not specified." + ProtocolValidator.HELP_MESSAGE);
        } else if (!request.getVersion().equals(ProtocolConstantsVersion2.VERSION_2) &&
				   !request.getVersion().equals(ProtocolConstantsVersion3.VERSION_3)) {
            throw new ProtocolException
                    (ProtocolStatusCode.VERSION_NOT_SUPPORTED,
                            "The web service API currently supports "
                                    + "version 2.0. or version 3.0" + ProtocolValidator.HELP_MESSAGE);
        }
    }
}