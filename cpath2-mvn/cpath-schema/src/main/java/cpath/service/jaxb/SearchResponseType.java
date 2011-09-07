package cpath.service.jaxb;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.*;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SearchResponseType", propOrder = {"error", "searchHit"})
public class SearchResponseType {

    protected ErrorType error;
    @XmlAttribute
    protected Integer numHitsBeforeRefined;
    protected List<SearchHitType> searchHit; // count to get actual no. hits!
    @XmlAttribute
    protected Integer pageNo; //search result page number

    public ErrorType getError() {
        return error;
    }

    public void setError(ErrorType value) {
        this.error = value;
    }

    public List<SearchHitType> getSearchHit() {
        if (searchHit == null) {
            searchHit = new ArrayList<SearchHitType>();
        }
        return this.searchHit;
    }
    
    public void setSearchHit(List<SearchHitType> searchHit) {
		this.searchHit = searchHit;
	}

	public Integer getNumHitsBeforeRefined() {
		return numHitsBeforeRefined;
	}

	public void setNumHitsBeforeRefined(Integer numHitsBeforeRefined) {
		this.numHitsBeforeRefined = numHitsBeforeRefined;
	}

	public Integer getPageNo() {
		return pageNo;
	}

	public void setPageNo(Integer pageNo) {
		this.pageNo = pageNo;
	}

}
