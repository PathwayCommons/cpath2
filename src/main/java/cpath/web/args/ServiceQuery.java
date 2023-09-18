package cpath.web.args;

public abstract class ServiceQuery {
  public ServiceQuery() {
  }

  @Override
  public String toString() {
    return cmd();
  }

  public abstract String cmd();

}
