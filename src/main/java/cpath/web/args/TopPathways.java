package cpath.web.args;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;
import java.util.Arrays;

public class TopPathways extends ServiceQuery {
  @NotBlank(
    message = "Parameter 'q' (a Lucene query string) is blank (not specified)."
  )
  @Schema(
    description = "Query string (supports Lucene query syntax).",
    required = true,
    example = "*"
  )
  private String q;

  @Schema(
    description = "Filter by organism, e.g., taxonomy id (recommended) or name.",
    example = "[\"9606\"]"
  )
  private String[] organism;

  @Schema(
    description = "Filter by data source name, id or uri.",
    example = "[\"reactome\"]"
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

}
