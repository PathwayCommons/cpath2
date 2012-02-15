package cpath.client.util;

import cpath.service.jaxb.ErrorResponse;

/**
 * See http://www.pathwaycommons.org/pc2-demo/#errors
 */
public class NoResultsFoundException extends CPathException {
    public NoResultsFoundException(ErrorResponse error) {
        super(error);
    }
}
