package cpath.web.args;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

import cpath.web.args.binding.BiopaxTypeEditor;
import io.swagger.v3.oas.annotations.media.Schema;
import org.biopax.paxtools.model.BioPAXElement;

import java.util.Arrays;

public class Search extends ServiceQuery {
  @NotBlank(message = "Parameter 'q' (a Lucene query string) is blank.")
  @Schema(
    description = """
        a keyword, name, identifier, or a Lucene query string;
        the index field names are: <var>uri, keyword, name, pathway, xrefid, datasource, organism</var>;
        e.g. <var>keyword</var> is the default search field that includes most of BioPAX element's properties
        and nested properties (e.g. a Complex can be found by one of its member's names or ECNumber).
        Search results, specifically the URIs, can be starting point for the graph, get, traverse queries.
        Search strings are case insensitive, except for <var>xrefid, uri</var>, or when it's enclosed in quotes.
        """,
    required = true,
    example = "xrefid:P*"
  )
  private String q;

  @Schema(
    description = """
        BioPAX class filter (<a href="/home#biopax_types" target="_blank">values</a>; case-insensitive).
        Note that some query filters, such as <code>&amp;type=biosource</code>
        (for most BioPAX UtilityClass, such as Score, Evidence), will not return any hits.
        So, use Entity (e.g., Pathway, Control, Protein) or EntityReference types
        (e.g. ProteinReference, SmallMoleculeReference) instead.
        """,
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
    description = """
        by-organism filter; values can be either the canonical names, e.g.
        <var>homo sapiens</var> or NCBI Taxon IDs, <var>9606</var>. If multiple values
        are provided, then the union of hits is returned; e.g.,
        <code>organism=9606&amp;organism=10016</code> results in both human and mouse related hits.
        See also: <a href="/home#organisms" target="_blank">supported species</a> (other organisms data,
        such as viruses and model organisms, can go together with e.g. human models that we integrated).
        """,
    example = "9606"
  )
  private String[] organism;

  @Schema(
    description = """
        filter by data source (an array of names, URIs
        of the <a href="/datasources" target="_blank">data sources</a> or any <var>Provenance</var>).
        If multiple data source values are specified, a union of hits from specified sources is returned;
        e.g., <code>datasource=reactome&amp;datasource=pid</code>.
        """,
    example = "reactome"
  )
  private String[] datasource;

  @Min(0)
  @Schema(
    description = "Pagination: the search result page number, N&gt;=0; default is 0.",
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
