package cpath.service.jaxb;

import java.math.BigInteger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType
public class ErrorType {

    @XmlElement(name = "error_code", required = true)
    protected BigInteger errorCode;
    @XmlElement(name = "error_msg", required = true)
    protected String errorMsg;
    @XmlElement(name = "error_details", required = true)
    protected String errorDetails;

    
    public BigInteger getErrorCode() {
        return errorCode;
    }


    public void setErrorCode(BigInteger value) {
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

}
