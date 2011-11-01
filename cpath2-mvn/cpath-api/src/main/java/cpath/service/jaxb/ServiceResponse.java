package cpath.service.jaxb;

import javax.xml.bind.annotation.*;

@XmlRootElement(name="serviceResponse")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ServiceResponse")
public class ServiceResponse {
	@XmlTransient
	private Object data; // BioPAX, String, List, or any other data
	
	public Object getData() {
		return data;
	}
	public void setData(Object data) {
		this.data = data;
	}
	
	/**
	 * True if there is no error response (yet), 
	 * and not empty response or data present.
	 * 
	 * @return
	 */
	@XmlTransient
	public boolean isEmpty() {
		return  (data == null || data.toString().trim().length() == 0);
	}
}
