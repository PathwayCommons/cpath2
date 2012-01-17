package cpath.webservice.args;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.biopax.paxtools.model.BioPAXElement;
import org.hibernate.validator.constraints.NotBlank;

public class Search {
	@NotNull(message="Parameter 'q' (Lucene full-text query string) is required!")
	@NotBlank(message="Parameter 'q' (Lucene full-text query string) is blank!")
	private String q;

	private Class<? extends BioPAXElement> type;

//	private OrganismDataSource[] organism;
//	private PathwayDataSource[] datasource;
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
	
	
	
//  disabled/commented out methods: any string value is now ok for filtering
//	public OrganismDataSource[] getOrganism() {
//		return organism;
//	}
//
//	public void setOrganism(OrganismDataSource[] organisms) {
//		this.organism = organisms;
//	}
//
//	public PathwayDataSource[] getDatasource() {
//		return datasource;
//	}
//
//	public void setDatasource(PathwayDataSource[] dataSources) {
//		this.datasource = dataSources;
//	}
//
//	public String[] datasources() {
//		String[] dsources = new String[datasource.length];
//		int i = 0;
//		for(PathwayDataSource d : datasource) {
//			dsources[i++] = d.asDataSource().getSystemCode();
//		}
//		return dsources;
//	}
//	
//	public String[] organisms() {
//		String[] orgs = new String[organism.length];
//		int i = 0;
//		for(OrganismDataSource d : organism) {
//			orgs[i++] = d.asDataSource().getSystemCode();
//		}
//		return orgs;
//	}
	
}
