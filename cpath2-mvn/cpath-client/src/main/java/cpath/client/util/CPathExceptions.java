package cpath.client.util;

import cpath.service.jaxb.ErrorResponse;

public final class CPathExceptions {
	
	// Suppress default constructor for noninstantiability
	private CPathExceptions() {
		throw new AssertionError();
	}
	
    public static CPathException newException(ErrorResponse error) {
        switch(error.getErrorCode().intValue()) {
            case 460:
                return new NoResultsFoundException(error);
            case 450:
                return new BadCommandException(error);
            case 452:
                return new BadRequestException(error);
            case 500:
            default:
            	return new InternalServerErrorException(error);
        }
    }
}
