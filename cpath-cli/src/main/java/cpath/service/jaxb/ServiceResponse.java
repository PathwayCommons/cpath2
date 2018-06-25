package cpath.service.jaxb;

import java.io.Serializable;

import javax.xml.bind.annotation.*;

// not instantiable, basic cpath2 xml response type
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ServiceResponse")
public abstract class ServiceResponse implements Serializable {
	@XmlTransient
	public abstract boolean isEmpty();
}
