package cpath.service.jaxb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TraverseEntry")
public class TraverseEntry implements Serializable {
    @XmlAttribute(required = true)
    private String uri;
    
    private List<String> value;

    public TraverseEntry() {
	}

    public String getUri() {
        return uri;
    }
    public void setUri(String value) {
        this.uri = value;
    }
	
	public List<String> getValue() {
		if(value == null) {
			value = new ArrayList<>();
		}
		return value;
	}
	public void setValue(List<String> value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return getValue().toString();
	}
	
	//package-private
	@XmlTransient
	boolean isEmpty() {
		return getValue().isEmpty();
	}
}
