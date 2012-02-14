package cpath.client.internal.util;

import cpath.service.jaxb.ErrorResponse;

import java.io.IOException;

/**
 * See http://www.pathwaycommons.org/pc2-demo/#errors
 */
public class CPathException extends IOException {
    private ErrorResponse error;

    public CPathException(ErrorResponse error) {
        super(error.toString());
        this.error = error;
    }

    public ErrorResponse getError() {
        return error;
    }
}
