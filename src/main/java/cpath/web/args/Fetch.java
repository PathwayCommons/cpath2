package cpath.web.args;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;
import org.biopax.paxtools.pattern.miner.SIFEnum;

import cpath.service.api.OutputFormat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

public class Fetch extends ServiceQuery {

  @NotNull(message = "Illegal Output Format")
  @Schema(
    description = "Output format name (default: BIOPAX)",
    example = "jsonld"
  )
  private OutputFormat format;

  // required at least one value
  @NotEmpty(message = "Provide at least one URI.")
  @Schema(
    description = "BioPAX entity URIs or standard identifiers (e.g., gene symbols)",
    required = true,
    example = "[\"TP53\"]"
  )
  private String[] uri;

  @Schema(
    description = "If format is SIF or TXT, one can specify interaction types to apply " +
      "(by default, it uses all the build-in patterns but 'neighbor-of')",
    example = "[\"interacts-with\"]" //custom editor maps this to "INTERACTS_WITH" and then to the SIFEnum instance
  )
  private SIFEnum[] pattern;

  @Schema(
    description =  "For the 'get' and 'graph' queries, whether to skip or not traversing into " +
      "sub-pathways in the result BioPAX sub-model.",
    example = "false"
  )
  private boolean subpw;

  public Fetch() {
    format = OutputFormat.BIOPAX; // default
    subpw = false;
    uri = new String[]{};
  }

  public OutputFormat getFormat() {
    return format;
  }

  public void setFormat(String format) {
    OutputFormat f = OutputFormat.typeOf(format.trim().toUpperCase());
    this.format = (f != null) ? f : OutputFormat.BIOPAX;
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

  public SIFEnum[] getPattern() {
    return pattern;
  }

  public void setPattern(String[] pattern) {
    if(pattern != null && pattern.length > 0)
      this.pattern = Arrays.stream(pattern)
        .distinct().map(p -> SIFEnum.typeOf(p.trim().toUpperCase()))// skip null (bad pattern name)
        .filter(Predicate.not(Objects::isNull)).toArray(SIFEnum[]::new);
    else
      this.pattern = null;
  }

  public boolean getSubpw() {
    return subpw;
  }
  public boolean isSubpw() {
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
    if (pattern != null && pattern.length > 0) {
      sb.append("; pat:").append(Arrays.toString(pattern));
    }
    return sb.toString();
  }

  @Override
  public String cmd() {
    return "get";
  }

}
