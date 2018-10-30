package cpath.service.api;

import java.util.ArrayList;
import java.util.List;


public enum Status {

    OK(200, "OK"),

    /**
     * Bad Request, e.g., missing or illegal arguments
     */
    BAD_REQUEST(400, "Bad Request (missing or illegal arguments)"),

    /**
     * Internal Server Error
     */
    INTERNAL_ERROR(500, "Internal Server Error"),

    /**
     * Internal Server Error
     */
    MAINTENANCE(503, "Server Is Temporarily Unavailable (Maintenance)");

    private final Integer code;
    private final String msg;
    

	/**
	 * Private Constructor
	 * 
	 * @param code
	 * @param msg
	 */
    Status(int code, String msg) {
		this.code = Integer.valueOf(code);
		this.msg = msg;
    }    
	
	
    /**
     * Gets status code
     *
     * @return Error Code.
     */
    public int getCode() {
        return code.intValue();
    }

    /**
     * Gets status message
     *
     * @return message.
     */
    public String getMsg() {
        return msg;
    }

    /**
     * Gets Complete List of all Status Codes.
     *
     * @return ArrayList of Status Objects.
     */
    public static List<String> getAllStatusCodes() {
        List<String> list = new ArrayList<>();
        for(Status statusCode : Status.values()) {
        	list.add(statusCode.name());
        }
        return list;
    }

}
