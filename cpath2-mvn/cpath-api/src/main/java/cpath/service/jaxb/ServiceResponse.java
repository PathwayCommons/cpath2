package cpath.service.jaxb;

import javax.xml.bind.annotation.*;

// not instantiable, basic cpath2 xml response type
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ServiceResponse")
public abstract class ServiceResponse {
	@XmlTransient
	public abstract boolean isEmpty();
}
