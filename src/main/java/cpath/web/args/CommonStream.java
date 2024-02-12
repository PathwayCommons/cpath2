package cpath.web.args;

import io.swagger.v3.oas.annotations.media.Schema;
import org.biopax.paxtools.query.algorithm.Direction;

public class CommonStream extends BaseGraph {

  @Schema(
      description = "Graph search direction (default: UNDIRECTED; cannot be BOTHSTREAM)",
      example = "undirected"
  )
  private Direction direction;

  public CommonStream() {
    super();
    direction = Direction.UNDIRECTED;
  }

  public Direction getDirection() {
    return direction;
  }

  public void setDirection(String direction) {
    Direction dir = Direction.typeOf(direction); //null when illegal value (also handles empty/null and register/case)
    //also exclude/replace BOTHSTREAM value
    this.direction = (dir != null && dir != Direction.BOTHSTREAM) ? dir : Direction.UNDIRECTED;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(super.toString());
    if (direction != null)
      sb.append("; dir:").append(direction);
    return sb.toString();
  }

  @Override
  public String cmd() {
    return "commonstream";
  }
}
