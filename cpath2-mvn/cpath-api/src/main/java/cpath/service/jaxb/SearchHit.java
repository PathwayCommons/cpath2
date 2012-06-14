package cpath.service.jaxb;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * Search hit java bean.
 * 
 * @author rodche
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
	name = "SearchHit", propOrder = {
    "uri",
    "biopaxClass",
    "name",
    "dataSource",
    "organism",
    "pathway",
    "excerpt"
})
public class SearchHit {
    @XmlElement(required = true)
    private String uri;
    @XmlElement(required = true)
    private String biopaxClass;
    private String name;
    private List<String> dataSource;
    private List<String> organism;
    private List<String> pathway;
    private String excerpt;


    public String getUri() {
        return uri;
    }
    public void setUri(String value) {
        this.uri = value;
    }
	
	public String getName() {
        return this.name;
    }
	public void setName(String name) {
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
   
    public List<String> getPathway() {
        if (pathway == null) {
            pathway = new ArrayList<String>();
        }
        return this.pathway;
    }
    public void setPathway(List<String> pathway) {
		this.pathway = pathway;
	}

    
    public String getExcerpt() {
        return excerpt;
    }
    public void setExcerpt(String value) {
        this.excerpt = value;
    }

    
    @Override
    public String toString() {
    	return (name != null) ? StringEscapeUtils.unescapeHtml(name) : uri;
    }
}
