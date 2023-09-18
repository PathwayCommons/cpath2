package cpath.web.args;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

import cpath.web.args.binding.BiopaxTypeEditor;
import io.swagger.v3.oas.annotations.media.Schema;
import org.biopax.paxtools.model.BioPAXElement;

import java.util.Arrays;

public class Search extends ServiceQuery {
  @NotBlank(message = "Parameter 'q' (a Lucene query string) is blank (not specified).")
  @Schema(
    description = "Query string (full-text search supports Lucene query syntax).",
    required = true,
    example = "xrefid:FGF*"
  )
  private String q;

  @Schema(
    description = "Filter by BioPAX L3 class name (BioPAX interface name, case-insensitive).",
    example = "pathway"
  )
  private String type;

  public String getType() {
    return type;
  }
  public void setType(String type) {
    this.type = type;
    this.biopaxClass = BiopaxTypeEditor.getSearchableBiopaxClassByName(type);
  }

  //this is set from request using custom property editor
  private Class<? extends BioPAXElement> biopaxClass;

  @Schema(
    description = "Filter by organism, e.g., taxonomy ID (recommended) or name.",
    example = "9606"
  )
  private String[] organism;

  @Schema(
    description = "Filter by data source name, id or uri.",
    example = "reactome"
  )
  private String[] datasource;

  @Min(0)
  @Schema(
    description = "Pagination: page number (>=0) of the full-text search results.",
    example = "0"
  )
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

  public Class<? extends BioPAXElement> getBiopaxClass() {
    return biopaxClass;
  }

  public void setBiopaxClass(Class<? extends BioPAXElement> biopaxClass) {
    this.biopaxClass = biopaxClass;
    if(biopaxClass != null) {
      this.type = biopaxClass.getSimpleName();
    }
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
    if (biopaxClass != null)
      sb.append("; t:").append(biopaxClass.getSimpleName());
    if (organism != null && organism.length > 0)
      sb.append("; org:").append(Arrays.toString(organism));
    if (datasource != null && datasource.length > 0)
      sb.append("; dts:").append(Arrays.toString(datasource));
    return sb.toString();
  }

  @Override
  public String cmd() {
    return "search";
  }

}
