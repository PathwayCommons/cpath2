package cpath.service.api;

import org.apache.commons.lang3.StringUtils;

public enum GraphType {
  NEIGHBORHOOD("search the neighborhood of given source set of nodes"),
  PATHSBETWEEN("find the paths between specific source set of states or entities within the boundaries of a specified length limit"),
  PATHSFROMTO("find the paths from a specific source set of states or entities to a specific target set of states or entities within the boundaries of a specified length limit"),
  COMMONSTREAM("search common downstream or common upstream of a specified set of entities based on the given directions within the boundaries of a specified length limit"),
  ;

  private final String description;

  public String getDescription() {
    return description;
  }

  GraphType(String description) {
    this.description = description;
  }

  public static GraphType typeOf(String tag)
  {
    if(StringUtils.isBlank(tag))
      return null;

    GraphType type = null;
    try {
      type = valueOf(tag.toUpperCase());
    }
    catch (IllegalArgumentException e){}

    return type;
  }
}