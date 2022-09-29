package cpath.web.args;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.Parameter;
import org.biopax.paxtools.pattern.miner.SIFEnum;

import cpath.service.api.OutputFormat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Get extends ServiceQuery {
  @NotNull(message = "Illegal Output Format")
  @Parameter(
    description = "Output format name (default: BIOPAX)",
    example = "JSONLD"
  )
  private OutputFormat format;

  // required at least one value
  @NotEmpty(message = "Provide at least one URI.")
  @Parameter(
    description = "Known BioPAX entity URIs or standard identifiers (e.g., gene symbols)",
    required = true,
    example = "TP53"
  )
  private String[] uri;

  @Parameter(
    description = "If format is SIF or TXT, one can specify interaction types to apply " +
      "(by default it uses all the build-in patterns but 'neighbor-of')",
    example = "interacts-with"
  )
  private SIFEnum[] pattern;

  @Parameter(
    description =  "For the 'get' and 'graph' queries, whether to skip or not traversing into " +
      "sub-pathways in the result BioPAX sub-model."
  )
  private boolean subpw;

  public Get() {
    format = OutputFormat.BIOPAX; // default
    subpw = false;
  }

  public OutputFormat getFormat() {
    return format;
  }

  public void setFormat(OutputFormat format) {
    this.format = format;
  }

  public String[] getUri() {
    return uri;
  }

  public void setUri(String[] uri) {
    Set<String> uris = new HashSet<>(uri.length);
    for (String item : uri) {
      if (item.contains(",")) {
        //split by ',' ignoring spaces and empty values (between ,,)
        for (String id : item.split("\\s*,\\s*", -1))
          uris.add(id);
      } else {
        uris.add(item.trim());
      }
    }
    this.uri = uris.toArray(new String[uris.size()]);
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
      .append("; uri:").append(Arrays.toString(uri));
    if (pattern != null && pattern.length > 0)
      sb.append("; pat:").append(Arrays.toString(pattern));

    return sb.toString();
  }

  @Override
  public String cmd() {
    return "get";
  }

  @Override
  public String outputFormat() {
    return format.name().toLowerCase();
  }
}
