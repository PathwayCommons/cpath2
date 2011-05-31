package cpath.service.jaxb;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class SearchHitType {

    @XmlAttribute
    protected String uri;
	
    protected Map<String, Object> props;

    public SearchHitType() {
    	this.props = new HashMap<String, Object>();
    }    

    public String getUri() {
        return uri;
    }

    public void setUri(String value) {
        this.uri = value;
    }
    
    public Map<String, Object> getProps() {
		return props;
	}
}
