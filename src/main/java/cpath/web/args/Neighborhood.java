package cpath.web.args;

import io.swagger.v3.oas.annotations.media.Schema;
import org.biopax.paxtools.query.algorithm.Direction;

public class Neighborhood extends BaseGraph {

  @Schema(
      description = "Graph search direction (default: UNDIRECTED)",
      example = "undirected"
  )
  private Direction direction;

  public Neighborhood() {
    super();
  }

  public Direction getDirection() {
    return direction;
  }

  public void setDirection(String direction) {
    this.direction = Direction.typeOf(direction); //null when illegal value (also handles empty/null and register/case)
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(super.toString());
    if (direction != null) {
      sb.append("; dir:").append(direction);
    }
    return sb.toString();
  }

  @Override
  public String cmd() {
    return "neighborhood";
  }
}
