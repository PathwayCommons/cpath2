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
    protected Long totalNumHits;
    protected List<SearchHitType> searchHit;


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

}
