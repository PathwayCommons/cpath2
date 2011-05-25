package cpath.service.jaxb;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SearchHitType", propOrder = {
    "uri",
    "biopaxClass",
    "name",
    "dataSource",
    "organism",
    "pathway",
    "excerpt"
})
public class SearchHitType {

    @XmlElement(required = true)
    protected String uri;
    @XmlElement(required = true)
    protected String biopaxClass;
    @XmlElement
    protected List<String> name;
    @XmlElement
    protected List<String> dataSource;
    @XmlElement
    protected List<String> organism;
    @XmlElement
    protected List<String> pathway;
    @XmlElement
    protected String excerpt;


    public String getUri() {
        return uri;
    }


    public void setUri(String value) {
        this.uri = value;
    }

    public List<String> getName() {
        if (name == null) {
            name = new ArrayList<String>();
        }
        return this.name;
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

    public List<String> getOrganism() {
        if (organism == null) {
            organism = new ArrayList<String>();
        }
        return this.organism;
    }

    public List<String> getPathway() {
        if (pathway == null) {
            pathway = new ArrayList<String>();
        }
        return this.pathway;
    }
   
    public String getExcerpt() {
        return excerpt;
    }

    public void setExcerpt(String value) {
        this.excerpt = value;
    }

}
