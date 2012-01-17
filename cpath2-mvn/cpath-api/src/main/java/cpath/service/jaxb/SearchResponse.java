package cpath.service.jaxb;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.*;

@XmlRootElement(name="searchResponse")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SearchResponse")
public class SearchResponse extends ServiceResponse {
    @XmlAttribute
    protected Integer numHits;
    
    protected List<SearchHit> searchHit; // count to get actual no. hits!
    
    @XmlAttribute
    protected Integer pageNo; //search result page number

    @XmlAttribute
    protected String comment;
	
	public String getComment() {
		return comment;
	}
	
	public void setComment(String comment) {
		this.comment = comment;
	}
    
    public List<SearchHit> getSearchHit() {
        if (searchHit == null) {
            searchHit = new ArrayList<SearchHit>();
        }
        return this.searchHit;
    }
    
    public void setSearchHit(List<SearchHit> searchHit) {
		this.searchHit = searchHit;
	}

    /**
     * The number of hits before filters or post-processing applied.
     * @return
     */
	public Integer getNumHits() {
		return numHits;
	}

	public void setNumHits(Integer numHits) {
		this.numHits = numHits;
	}

	public Integer getPageNo() {
		return pageNo;
	}

	public void setPageNo(Integer pageNo) {
		if(pageNo >= 0)
			this.pageNo = pageNo;
		else 
			throw new IllegalArgumentException("Negative values are not supported!");
	}

	@Override
	public boolean isEmpty() {
		return getSearchHit().isEmpty();
	}
}
