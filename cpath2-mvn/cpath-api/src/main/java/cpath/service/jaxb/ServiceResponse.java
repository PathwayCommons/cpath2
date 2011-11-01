package cpath.service.jaxb;

import javax.xml.bind.annotation.*;

@XmlRootElement(name="serviceResponse")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ServiceResponse")
public abstract class ServiceResponse {
	@XmlTransient
	public abstract boolean isEmpty();
}
