package cpath.webservice.args;

import javax.validation.constraints.Min;

import org.biopax.paxtools.model.BioPAXElement;
import org.hibernate.validator.constraints.NotBlank;

public class Search {
	@NotBlank(message="Parameter 'q' (a Lucene query string) is blank (not specified).")
	private String q;

	private Class<? extends BioPAXElement> type;

	private String[] organism;
	private String[] datasource;
	
	@Min(0)
	private Integer page;

	public Search() {
		page = 0;
	}

	public String getQ() {
		return q;
	}

	public void setQ(String q) {
		this.q = q;
	}

	public Class<? extends BioPAXElement> getType() {
		return type;
	}

	public void setType(Class<? extends BioPAXElement> type) {
		this.type = type;
	}


	public String[] getOrganism() {
		return organism;
	}

	public void setOrganism(String[] organism) {
		this.organism = organism;
	}

	public String[] getDatasource() {
		return datasource;
	}

	public void setDatasource(String[] datasource) {
		this.datasource = datasource;
	}

	public Integer getPage() {
		return page;
	}

	public void setPage(Integer page) {
		this.page = page;
	}
		
}
