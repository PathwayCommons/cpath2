package cpath.service.jaxb;

import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "Response")
public abstract class Response {
	@XmlTransient
	public abstract boolean isEmpty();
}
