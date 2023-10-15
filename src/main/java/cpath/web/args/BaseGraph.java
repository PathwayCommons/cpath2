package cpath.web.args;

import cpath.service.api.OutputFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import org.biopax.paxtools.pattern.miner.SIFEnum;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

public abstract class BaseGraph extends ServiceQuery {

  @NotEmpty(message = "Provide at least one source URI.")
  @Schema(
    description = "Source BioPAX entity URIs or IDs (e.g., gene symbols)",
    required = true,
    example = "[\"Q16602\"]"
  )
  private String[] source;

  @Min(1) //note: this allows for null
  @Schema(
    description = "Graph search distance limit (default: 1)",
    example = "1"
  )
  private Integer limit;

  @NotNull(message = "Illegal Output Format")
  @Schema(
    description = "Output format name (default: BIOPAX)",
    example = "jsonld"
  )
  private OutputFormat format;

  @Schema(
    description = "Filter by organism, e.g., taxonomy ID (recommended) or name.",
    example = "[\"9606\"]"
  )
  private String[] organism;

  @Schema(
    description = "Filter by data source name, id or uri.",
    example = "[\"reactome\"]"
  )
  private String[] datasource;

  @Schema(
    description = "If format is SIF or TXT, one can specify interaction types to apply",
    example = "[\"interacts-with\",\"used-to-produce\"]" //editor/setter maps this to "INTERACTS_WITH","USED_TO_PRODUCE" SIFEnum instances
  )
  private SIFEnum[] pattern;

  @Schema(
    description = "For the 'get' and 'graph' queries, whether to skip or not traversing " +
      "into sub-pathways in the result BioPAX sub-model."
  )
  private boolean subpw;

  public BaseGraph() {
    //set default vales
    format = OutputFormat.BIOPAX;
    limit = 1;
    subpw = false;
  }

  public OutputFormat getFormat() {
    return format;
  }

  public void setFormat(String format) {
    OutputFormat f = OutputFormat.typeOf(format.trim().toUpperCase());
    this.format = (f != null) ? f : OutputFormat.BIOPAX;
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

  public Integer getLimit() {
    return limit;
  }

  public void setLimit(Integer limit) {
    this.limit = limit;
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

  public SIFEnum[] getPattern() {
    return pattern;
  }

  public void setPattern(String[] pattern) {
    if(pattern != null && pattern.length > 0)
      this.pattern = Arrays.stream(pattern)
          .distinct().map(p -> SIFEnum.typeOf(p.trim().toUpperCase()))//skip null (bad pattern value)
          .filter(Predicate.not(Objects::isNull)).toArray(SIFEnum[]::new);
    else
      this.pattern = null;
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
    if (limit != null)
      sb.append("; lim:").append(limit);
    if (organism != null && organism.length > 0)
      sb.append("; org:").append(Arrays.toString(organism));
    if (datasource != null && datasource.length > 0)
      sb.append("; dts:").append(Arrays.toString(datasource));
    if (pattern != null && pattern.length > 0)
      sb.append("; pat:").append(Arrays.toString(pattern));
    return sb.toString();
  }

}
