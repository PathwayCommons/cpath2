package cpath.webservice.args;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.biopax.paxtools.model.BioPAXElement;
import org.hibernate.validator.constraints.NotBlank;

public class Search {
	@NotNull(message="Parameter 'q' (Lucene full-text query string) is required!")
	@NotBlank(message="Parameter 'q' (Lucene full-text query string) is blank!")
	private String q;
	
	private Class<? extends BioPAXElement> type;
	@Valid
	private OrganismDataSource[] organism;
	@Valid
	private PathwayDataSource[] datasource;
	
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

	public OrganismDataSource[] getOrganism() {
		return organism;
	}

	public void setOrganism(OrganismDataSource[] organisms) {
		this.organism = organisms;
	}

	public PathwayDataSource[] getDatasource() {
		return datasource;
	}

	public void setDatasource(PathwayDataSource[] dataSources) {
		this.datasource = dataSources;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

}
