package cpath.web.args;


import io.swagger.annotations.ApiParam;

import javax.validation.constraints.NotBlank;
import java.util.Arrays;

public class TopPathways extends ServiceQuery {
  @NotBlank(
    message = "Parameter 'q' (a Lucene query string) is blank (not specified)."
  )
  @ApiParam(
    value = "Query string (supports Lucene query syntax).",
    required = true,
    example = "*"
  )
  private String q;

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

  public TopPathways() {
  }

  public String getQ() {
    return q;
  }

  public void setQ(String q) {
    this.q = q;
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


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(super.toString());
    if (q != null && !q.isEmpty())
      sb.append("; q:").append(q);
    if (organism != null && organism.length > 0)
      sb.append("; org:").append(Arrays.toString(organism));
    if (datasource != null && datasource.length > 0)
      sb.append("; dts:").append(Arrays.toString(datasource));
    return sb.toString();
  }

  @Override
  public String cmd() {
    return "top_pathways";
  }

  @Override
  public String outputFormat() {
    return "xml"; //default
  }
}
