package cpath.service.jaxb;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Search hit java bean.
 * 
 * @author rodche
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SearchHitType", propOrder = {
    "uri",
    "actualHitUri",
    "biopaxClass",
    "name",
    "dataSource",
    "organism",
    //"pathway", //feature (temporary) disabled
    "excerpt"
})
public class SearchHitType {
    @XmlElement(required = true)
    protected String uri;
    protected String actualHitUri;
    @XmlElement(required = true)
    protected String biopaxClass;
    protected List<String> name;
    protected List<String> dataSource;
    protected List<String> organism;
//    protected List<String> pathway;
    protected String excerpt;


    public String getUri() {
        return uri;
    }
    public void setUri(String value) {
        this.uri = value;
    }
    
    public String getActualHitUri() {
		return actualHitUri;
	}
	public void setActualHitUri(String actualHitUri) {
		this.actualHitUri = actualHitUri;
	}
	
	public List<String> getName() {
        if (name == null) {
            name = new ArrayList<String>();
        }
        return this.name;
    }
	public void setName(List<String> name) {
		this.name = name;
	}

    public String getBiopaxClass() {
        return biopaxClass;
    }

    public void setBiopaxClass(String value) {
        this.biopaxClass = value;
    }

    public List<String> getDataSource() {
        if (dataSource == null) {
            dataSource = new ArrayList<String>();
        }
        return this.dataSource;
    }
    public void setDataSource(List<String> dataSource) {
		this.dataSource = dataSource;
	}

    
    public List<String> getOrganism() {
        if (organism == null) {
            organism = new ArrayList<String>();
        }
        return this.organism;
    }
    public void setOrganism(List<String> organism) {
		this.organism = organism;
	}

/*    
    public List<String> getPathway() {
        if (pathway == null) {
            pathway = new ArrayList<String>();
        }
        return this.pathway;
    }
    public void setPathway(List<String> pathway) {
		this.pathway = pathway;
	}
*/   
    public String getExcerpt() {
        return excerpt;
    }
    public void setExcerpt(String value) {
        this.excerpt = value;
    }

}
