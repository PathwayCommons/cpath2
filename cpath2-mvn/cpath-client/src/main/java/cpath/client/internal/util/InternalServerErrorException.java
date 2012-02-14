package cpath.client.internal.util;

import cpath.service.jaxb.ErrorResponse;

/**
 * See http://www.pathwaycommons.org/pc2-demo/#errors
 */
public class InternalServerErrorException extends CPathException {
    public InternalServerErrorException(ErrorResponse error) {
        super(error);
    }
}
