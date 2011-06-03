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
	private OrganismDataSource[] organisms;
	@Valid
	private PathwayDataSource[] dataSources;
	private String[] pathwayURIs;

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

	public OrganismDataSource[] getOrganisms() {
		return organisms;
	}

	public void setOrganisms(OrganismDataSource[] organisms) {
		this.organisms = organisms;
	}

	public PathwayDataSource[] getDataSources() {
		return dataSources;
	}

	public void setDataSources(PathwayDataSource[] dataSources) {
		this.dataSources = dataSources;
	}

	public String[] getPathwayURIs() {
		return pathwayURIs;
	}

	public void setPathwayURIs(String[] pathwayURIs) {
		this.pathwayURIs = pathwayURIs;
	}
}
