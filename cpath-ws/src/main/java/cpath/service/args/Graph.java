package cpath.service.args;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.biopax.paxtools.pattern.miner.SIFType;
import org.biopax.paxtools.query.algorithm.Direction;
import org.hibernate.validator.constraints.NotEmpty;

import cpath.service.GraphType;
import cpath.service.OutputFormat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Graph extends ServiceQuery {
	@NotNull(message="Parameter 'kind' is required.")
	private GraphType kind; //required!
	
	@NotEmpty(message="Provide at least one source URI.")
	private String[] source;
	
	private String[] target;
	
	@Min(1) //note: this allows for null
	private Integer limit;
	
	private Direction direction;
	
	@NotNull(message="Illegal Output Format")
	private OutputFormat format;
	
	private String[] organism;
	
	private String[] datasource;

	private SIFType[] pattern;

	private boolean subpw;

	public Graph() {
		format = OutputFormat.BIOPAX; // default
		limit = 1;
		subpw = false;
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
		Set<String> uris = new HashSet<String>(source.length);
		for(String item : source) {
			if(item.contains(",")) {
				//split by ',' ignoring spaces and empty values (between ,,)
				for(String id : item.split("\\s*,\\s*", -1))
					uris.add(id);
			} else {
				uris.add(item);
			}
		}
		this.source = uris.toArray(new String[uris.size()]);
	}

	public String[] getTarget() {
		return target;
	}

	public void setTarget(String[] target) {
		Set<String> uris = new HashSet<String>(target.length);
		for(String item : target) {
			if(item.contains(",")) {
				//split by ',' ignoring spaces and empty values (between ,,)
				for(String id : item.split("\\s*,\\s*", -1))
					uris.add(id);
			} else {
				uris.add(item.trim());
			}
		}
		this.target = uris.toArray(new String[uris.size()]);
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

	//SIF Types
	public SIFType[] getPattern() {
		return pattern;
	}

	public void setPattern(SIFType[] pattern) {
		this.pattern = pattern;
	}

	public boolean getSubpw() {
		return subpw;
	}

	public void setSubpw(boolean subpw) {
		this.subpw = subpw;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString())
			.append(" for:").append(format)
			.append("; spw:").append(subpw)
			.append("; src:").append(Arrays.toString(source));
		if(target!=null && target.length>0)
		 sb.append("; tgt:").append(Arrays.toString(target));
		if(limit!=null)
			sb.append("; lim:").append(limit);
		if(organism!=null && organism.length>0)
			sb.append("; org:").append(Arrays.toString(organism));
		if(datasource!=null && datasource.length>0)
			sb.append("; dts:").append(Arrays.toString(datasource));
		if(direction!=null)
			sb.append("; dir:").append(direction);
		if(pattern!=null && pattern.length>0)
			sb.append("; pat:").append(Arrays.toString(pattern));
		return sb.toString();
	}

	@Override
	public String getCommand() {
		return kind.toString();
	}

	@Override
	public String getFormatName() {
		return format.name().toLowerCase();
	}
}
