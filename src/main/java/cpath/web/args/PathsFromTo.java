package cpath.web.args;

import io.swagger.v3.oas.annotations.media.Schema;
import org.biopax.paxtools.query.algorithm.LimitType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PathsFromTo extends BaseGraph {

  @Schema(
    description = "Target BioPAX entity URIs/IDs (if not present, then 'pathsbetween' algorithm is used instead)",
    example = "[]"
  )
  private String[] target;

  @Schema(
      description = "Limit Type: 'normal', 'shortest-plus-k'",
      example = "normal"
  )
  private LimitType limitType;

  public PathsFromTo() {
    super();
    limitType = LimitType.NORMAL;
  }

  public LimitType getLimitType() {
    return limitType;
  }

  public void setLimitType(String limitType) {
    LimitType limt = LimitType.typeOf(limitType);
    this.limitType = (limt != null) ? limt : LimitType.NORMAL;
  }

  public String[] getTarget() {
    return target;
  }

  public void setTarget(String[] target) {
    Set<String> uris = new HashSet<>(target.length);
    for (String item : target) {
      if (item.contains(",")) {
        //split by ',' ignoring spaces and empty values (between ,,)
        for (String id : item.split("\\s*,\\s*", -1)) {
          uris.add(id);
        }
      } else {
        uris.add(item.trim());
      }
    }
    this.target = uris.toArray(new String[uris.size()]);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(super.toString());
    if (limitType != null)
      sb.append("; limt:").append(limitType);
    if (target != null && target.length > 0)
      sb.append("; tgt:").append(Arrays.toString(target));
    return sb.toString();
  }

  @Override
  public String cmd() {
    return "pathsfromto";
  }

}
