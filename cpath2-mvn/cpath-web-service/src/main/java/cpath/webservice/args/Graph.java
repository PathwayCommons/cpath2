package cpath.webservice.args;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.biopax.paxtools.query.algorithm.Direction;
import org.hibernate.validator.constraints.NotEmpty;

import cpath.service.GraphType;
import cpath.service.OutputFormat;

public class Graph {
	@NotNull(message="Parameter 'kind' is required!")
	private GraphType kind; //required!
	
	@NotNull(message="Parameter 'source' is required!")
	@NotEmpty(message="At least one source URI has to be set.")
	private String[] source;
	
	private String[] target;
	
	@Min(1) //note: this allows for null
	private Integer limit;
	
	private Direction direction;
	
	@NotNull(message="Illegal Output Format")
	private OutputFormat format;

	private String[] organism;

	private String[] datasource;

	public Graph() {
		format = OutputFormat.BIOPAX; // default
		limit = 1;
	}

	public OutputFormat getFormat() {
		return format;
	}

	public void setFormat(OutputFormat format) {
		this.format = format;
	}

	public GraphType getKind() {
		return kind;
	}

	public void setKind(GraphType kind) {
		this.kind = kind;
	}

	public String[] getSource() {
		return source;
	}

	public void setSource(String[] source) {
		this.source = source;
	}

	public String[] getTarget() {
		return target;
	}

	public void setTarget(String[] target) {
		this.target = target;
	}

	public Integer getLimit() {
		return limit;
	}

	public void setLimit(Integer limit) {
		this.limit = limit;
	}

	public Direction getDirection() {
		return direction;
	}

	public void setDirection(Direction direction) {
		this.direction = direction;
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
}
