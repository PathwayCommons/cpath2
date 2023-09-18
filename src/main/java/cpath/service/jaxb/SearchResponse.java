package cpath.service.jaxb;

import jakarta.xml.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@XmlRootElement(name="searchResponse")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SearchResponse")
public class SearchResponse extends ServiceResponse {
    @XmlAttribute
    private Long numHits;
    
    @XmlAttribute
    private Integer maxHitsPerPage;
    
    private List<SearchHit> searchHit; // count to get actual no. hits!
    
    @XmlAttribute
    private Integer pageNo; //search result page number

    @XmlAttribute
    private String comment;

	@XmlAttribute
	private String version;
    
    @XmlTransient
    private Set<String> providers; //pathway data provider standard names (for logging/stats)
	
	public SearchResponse() {
	}
    
  public String getComment() {
		return comment;
	}
	
	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public List<SearchHit> getSearchHit() {
        if (searchHit == null) {
            searchHit = new ArrayList<>();
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
	public Long getNumHits() {
		return numHits;
	}

	public void setNumHits(Long numHits) {
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
	@XmlTransient
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
			return (int)((numHits-1)/maxHitsPerPage) + 1;
		else 
			return 0;
	}
	

	@XmlTransient
	public Set<String> getProviders() {
		if(providers == null)
			providers = new HashSet<>();
		
		return providers;
	}
	public void setProviders(Set<String> providers) {
		this.providers = providers;
	}
	
	
	public Set<String> provenanceUris() {
		Set<String> provUris = new HashSet<>();
		
		if(searchHit != null) {
			for(SearchHit hit : searchHit) {
				//for Prov. there are names instead of URIs
				if(!"Provenance".equalsIgnoreCase(hit.getBiopaxClass()))
					provUris.addAll(hit.getDataSource());
				else 
					provUris.add(hit.getUri());
			}
		}
		
		return provUris;
	}
}
