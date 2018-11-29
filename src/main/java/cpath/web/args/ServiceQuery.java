package cpath.web.args;

import io.swagger.annotations.ApiParam;

/**
 * Created by igor on 23/06/16.
 */
public abstract class ServiceQuery {

  @ApiParam(
    value = "User or app name (for service analytics)",
    required = false
  )
  private String user;

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  @Override
  public String toString() {
    return cmd() + ((user != null) ? " cli:" + user + ";" : "");
  }

  public abstract String cmd();

  public abstract String outputFormat();
}
