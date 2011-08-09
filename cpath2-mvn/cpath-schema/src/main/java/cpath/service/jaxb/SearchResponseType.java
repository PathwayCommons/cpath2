package cpath.service.jaxb;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.*;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SearchResponseType", propOrder = {
    "error",
    "totalNumHits",
    "searchHit"
})
public class SearchResponseType {

    protected ErrorType error;
    protected Long totalNumHits; // actually means - "raw" hits before filters and lookup (for parent ent.) applied!
    protected List<SearchHitType> searchHit; // count these to get actual num hits!
    //TODO add "page" number (search pagination)
    //TODO add "numHitsPerPage" (== cpath.properties variable 'maxSearchHitsPerPage')
    //TODO update the schema file in the cpath-webdocs/src/main/webapp/resources/schemas/

    public ErrorType getError() {
        return error;
    }


    public void setError(ErrorType value) {
        this.error = value;
    }

    public Long getTotalNumHits() {
        return totalNumHits;
    }

    public void setTotalNumHits(Long value) {
        this.totalNumHits = value;
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

}
