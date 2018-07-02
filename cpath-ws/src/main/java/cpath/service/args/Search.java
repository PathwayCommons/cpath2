package cpath.service.args;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

import org.biopax.paxtools.model.BioPAXElement;

import java.util.Arrays;

public class Search extends ServiceQuery {
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

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString()).append(" q:").append(q).append("; p:").append(page);
		if(type!=null)
			sb.append("; t:").append(type.getSimpleName());
		if(organism!=null && organism.length>0)
			sb.append("; org:").append(Arrays.toString(organism));
		if(datasource!=null && datasource.length>0)
			sb.append("; dts:").append(Arrays.toString(datasource));
		return sb.toString();
	}

	@Override
	public String cmd() {
		return "search";
	}

	@Override
	public String outputFormat() {
		return "json"; //default
	}
}
