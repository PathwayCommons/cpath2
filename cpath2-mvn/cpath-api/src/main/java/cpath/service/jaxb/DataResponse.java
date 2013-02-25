package cpath.service.jaxb;

import javax.xml.bind.annotation.*;

/**
 * An internal service bean, any-data response type.
 * 
 * (Despite being annotated and in the cpath.beans.jaxb package, 
 * in practice, this is not for XML marshalling/unmarshalling, but
 * rather for internal use and information.
 *
 */
@XmlRootElement(name="dataResponse")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DataResponse")
public class DataResponse extends ServiceResponse {
//	@XmlTransient
	private Object data; // BioPAX OWL, String, List, or any other data
	
	public DataResponse() {
	}
	
	public Object getData() {
		return data;
	}
	public void setData(Object data) {
		this.data = data;
	}
	
	@XmlTransient
	public boolean isEmpty() {
		return  (data == null || data.toString().trim().length() == 0);
	}
}
