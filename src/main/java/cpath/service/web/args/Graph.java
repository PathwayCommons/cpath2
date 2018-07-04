package cpath.service.web.args;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiParam;
import org.biopax.paxtools.pattern.miner.SIFEnum;
import org.biopax.paxtools.query.algorithm.Direction;

import cpath.service.api.GraphType;
import cpath.service.api.OutputFormat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Graph extends ServiceQuery {
  @NotNull(message = "Parameter 'kind' is required.")
  @ApiParam(
    value = "BioPAX graph traversal type.",
    required = true,
    example = "PATHSBETWEEN"
  )
  private GraphType kind; //required!

  @NotEmpty(message = "Provide at least one source URI.")
  @ApiParam(
    value = "Source BioPAX entity URIs or standard identifiers (e.g., gene symbols)",
    required = true,
    example = "TP53"
  )
  private String[] source;

  @ApiParam(
    value = "Target BioPAX entity URIs or standard identifiers (e.g., gene symbols);this parameter works only with kind=PATHSFROMTO graph queries.",
    required = false,
    example = "TP53"
  )
  private String[] target;

  @Min(1) //note: this allows for null
  @ApiParam(
    value = "Graph search distance limit",
    required = false,
    defaultValue = "1"
  )
  private Integer limit;

  @ApiParam(
    value = "Graph search direction.",
    required = false,
    defaultValue = "UNDIRECTED",
    example = "BOTHSTREAM"
  )
  private Direction direction;

  @NotNull(message = "Illegal Output Format")
  @ApiParam(
    value = "Output format name.",
    required = false,
    defaultValue = "BIOPAX"
  )
  private OutputFormat format;

  @ApiParam(
    value = "Filter by organism, e.g., taxonomy ID (recommended) or name.",
    example = "9606"
  )
  private String[] organism;

  @ApiParam(
    value = "Filter by data source name, id or uri.",
    example = "reactome"
  )
  private String[] datasource;

  @ApiParam(
    value = "If format is SIF or TXT, one can specify interaction types to apply",
    example = "interacts-with"
  )
  private SIFEnum[] pattern;

  @ApiParam(
    value = "For the 'get' and 'graph' queries, whether to skip or not traversing into sub-pathways in the result BioPAX sub-model.",
    defaultValue = "false"
  )
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
    Set<String> uris = new HashSet<>(source.length);
    for (String item : source) {
      if (item.contains(",")) {
        //split by ',' ignoring spaces and empty values (between ,,)
        for (String id : item.split("\\s*,\\s*", -1))
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
    Set<String> uris = new HashSet<>(target.length);
    for (String item : target) {
      if (item.contains(",")) {
        //split by ',' ignoring spaces and empty values (between ,,)
        for (String id : item.split("\\s*,\\s*", -1))
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
  public SIFEnum[] getPattern() {
    return pattern;
  }

  public void setPattern(SIFEnum[] pattern) {
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
    if (target != null && target.length > 0)
      sb.append("; tgt:").append(Arrays.toString(target));
    if (limit != null)
      sb.append("; lim:").append(limit);
    if (organism != null && organism.length > 0)
      sb.append("; org:").append(Arrays.toString(organism));
    if (datasource != null && datasource.length > 0)
      sb.append("; dts:").append(Arrays.toString(datasource));
    if (direction != null)
      sb.append("; dir:").append(direction);
    if (pattern != null && pattern.length > 0)
      sb.append("; pat:").append(Arrays.toString(pattern));
    return sb.toString();
  }

  @Override
  public String cmd() {
    return kind.toString();
  }

  @Override
  public String outputFormat() {
    return format.name().toLowerCase();
  }
}
