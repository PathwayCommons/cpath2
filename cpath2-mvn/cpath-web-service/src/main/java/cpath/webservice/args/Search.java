package cpath.webservice.args;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.biopax.paxtools.model.BioPAXElement;
import org.hibernate.validator.constraints.NotBlank;

public class Search {
	@NotNull(message="Parameter 'q' (Lucene full-text query string) is required!")
	@NotBlank
	private String q;
	
	private Class<? extends BioPAXElement> type;
	@Valid
	private OrganismDataSource[] organism;
	@Valid
	private PathwayDataSource[] datasource;
	//private String[] pathwayURIs;

	public Search() {
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

//	public String[] getPathwayURIs() {
//		return pathwayURIs;
//	}
//
//	public void setPathwayURIs(String[] pathwayURIs) {
//		this.pathwayURIs = pathwayURIs;
//	}
}
