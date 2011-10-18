package cpath.service.jaxb;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.*;

@XmlRootElement(name="searchResponse")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SearchResponse")
public class SearchResponse extends Response {
    @XmlAttribute
    protected Integer maxHits;
    
    protected List<SearchHit> searchHit; // count to get actual no. hits!
    
    @XmlAttribute
    protected Integer pageNo; //search result page number

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
	public Integer getMaxHits() {
		return maxHits;
	}

	public void setMaxHits(Integer maxHits) {
		this.maxHits = maxHits;
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
