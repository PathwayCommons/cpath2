package cpath.service.jaxb;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.*;

@XmlRootElement(name="searchResponse")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SearchResponse")
public class SearchResponse extends ServiceResponse {
    @XmlAttribute
    private Integer numHits;
    
    @XmlAttribute
    private Integer maxHitsPerPage;
    
    private List<SearchHit> searchHit; // count to get actual no. hits!
    
    @XmlAttribute
    private Integer pageNo; //search result page number

    @XmlAttribute
    private String comment;
	
	public SearchResponse() {
	}
    
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
     * The total number of hits
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
			throw new IllegalArgumentException("Negative values are not supported");
	}

	@Override
	public boolean isEmpty() {
		return getSearchHit().isEmpty();
	}

    /**
     * The number of hits per page.
     * @return
     */
	public Integer getMaxHitsPerPage() {
		return maxHitsPerPage;
	}

	public void setMaxHitsPerPage(Integer maxHitsPerPage) {
		this.maxHitsPerPage = maxHitsPerPage;
	}
	

	/**
	 * Calculates the total number of search result
	 * pages using current  {@link #getMaxHitsPerPage()} and {@link #numHits}
	 * 
	 * @return no. pages or 0 (if there're no hits yet, or it's a wrong state)
	 */
	public int numPages() {
		if(numHits > 0 && maxHitsPerPage > 0)
			return (numHits-1)/maxHitsPerPage + 1;
		else 
			return 0;
	}
}
