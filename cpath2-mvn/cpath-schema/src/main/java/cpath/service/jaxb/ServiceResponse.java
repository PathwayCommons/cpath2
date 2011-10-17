package cpath.service.jaxb;

import static cpath.service.Status.*;

import javax.xml.bind.annotation.*;

@XmlRootElement(name="serviceResponse")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ServiceResponse")
public class ServiceResponse {
	@XmlTransient
	private Object data; // BioPAX, String, List, or any other data
	
    @XmlAttribute
    protected String query;
    
    @XmlAttribute
    protected String comment;

    @XmlElementRef
    protected Response response;
	
	public String getQuery() {
		return query;
	}
	public void setQuery(String query) {
		this.query = query;
	}
	
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
	
	public Response getResponse() {
		return response;
	}
	public void setResponse(Response response) {
		this.response = response;
	}
	
	/* xml transient utility properties */
	
	public Object getData() {
		return data;
	}
	public void setData(Object data) {
		this.data = data;
	}
	
	
	/**
	 * True if it contains an error response object.
	 *  
	 * @return
	 */
	@XmlTransient
	public boolean isError() {
		return response instanceof ErrorResponse;
	}
	
	
	/**
	 * True if there is no error response (yet), 
	 * and not empty response or data present.
	 * 
	 * @return
	 */
	@XmlTransient
	public boolean isEmpty() {
		return  !isError() // no error (error is not an empty result)
			&& (data == null || data.toString().trim().length() == 0) 
			&& (response == null || response.isEmpty());
	}
	
	/**
	 * Sets "no results found" status (errorResponse)
	 */
	public void setNoResultsFoundResponse(String details) {
		if(details==null) details = "Nothing found!";
		setResponse(NO_RESULTS_FOUND.errorResponse(details));
	}
	
	
	public void setExceptionResponse(Exception e) {
		setResponse(INTERNAL_ERROR.errorResponse(e));
	}
}
