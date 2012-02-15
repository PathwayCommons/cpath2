package cpath.client.util;

import cpath.service.jaxb.ErrorResponse;

import java.io.IOException;


public abstract class CPathException extends IOException {
    private ErrorResponse error;

    public CPathException(ErrorResponse error) {
        super(error.toString());
        this.error = error;
    }

    public ErrorResponse getError() {
        return error;
    }
}
