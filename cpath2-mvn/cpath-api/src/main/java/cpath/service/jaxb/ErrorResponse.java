package cpath.service.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import cpath.service.Status;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ErrorResponse")
@XmlRootElement(name = "errorResponse")
public class ErrorResponse extends ServiceResponse {
    @XmlElement(name = "error_code", required = true)
    private Integer errorCode;
    @XmlElement(name = "error_msg", required = true)
    private String errorMsg;
    @XmlElement(name = "error_details", required = true)
    private String errorDetails;

    
    public Integer getErrorCode() {
        return errorCode;
    }


    public void setErrorCode(Integer value) {
        this.errorCode = value;
    }


    public String getErrorMsg() {
        return errorMsg;
    }


    public void setErrorMsg(String value) {
        this.errorMsg = value;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(String value) {
        this.errorDetails = value;
    }
	

	public boolean isStatus(Status statusCode) {
		return statusCode.equals(this.errorCode);
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + 
			errorCode + " " + errorMsg + " - " + errorDetails;
	}


	@Override
	public boolean isEmpty() {
		return false;
	}
}
