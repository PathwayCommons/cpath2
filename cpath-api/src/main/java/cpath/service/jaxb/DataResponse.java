package cpath.service.jaxb;

import java.util.Set;

import javax.xml.bind.annotation.*;

/**
 * An internal service bean, any-data response type.
 * 
 * This is normally not for XML marshalling/unmarshalling, 
 * but rather for exchanging data between DB, service, and web tiers.
 *
 */
//@XmlRootElement(name="dataResponse")
//@XmlAccessorType(XmlAccessType.FIELD)
//@XmlType(name = "DataResponse")
@XmlTransient
public class DataResponse extends ServiceResponse {

	private Object data; // BioPAX OWL, String, List, or any other data
	private Set<String> providers; //pathway data provider names (for logging/stats)
	
	public DataResponse() {
	}
	
	public Object getData() {
		return data;
	}
	public void setData(Object data) {
		this.data = data;
	}
	
	@Override
	@XmlTransient
	public boolean isEmpty() {
		return  (data == null || data.toString().trim().isEmpty());
	}
	
	@XmlTransient
	public Set<String> getProviders() {
		return providers;
	}
	public void setProviders(Set<String> providers) {
		this.providers = providers;
	}
}
