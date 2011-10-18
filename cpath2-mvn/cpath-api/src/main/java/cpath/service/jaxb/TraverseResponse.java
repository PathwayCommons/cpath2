package cpath.service.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.*;

@XmlRootElement(name="traverseResponse")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TraverseResponse")
public class TraverseResponse extends Response {
    @XmlAttribute
    protected String propertyPath;

    protected List<TraverseEntry> traverseEntry;
    
	public String getPropertyPath() {
		return propertyPath;
	}
	public void setPropertyPath(String propertyPath) {
		this.propertyPath = propertyPath;
	}

	public List<TraverseEntry> getTraverseEntry() {
		if(traverseEntry == null) {
			traverseEntry = new ArrayList<TraverseEntry>();
		}
		return traverseEntry;
	}
	public void setTraverseEntry(List<TraverseEntry> traverseEntry) {
		this.traverseEntry = traverseEntry;
	}
	
	@Override
	public boolean isEmpty() {
		return getTraverseEntry().isEmpty();
	} 

}
