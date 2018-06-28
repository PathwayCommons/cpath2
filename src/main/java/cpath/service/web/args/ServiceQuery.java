package cpath.service.web.args;

/**
 * Created by igor on 23/06/16.
 */
public abstract class ServiceQuery {
    private String user;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return cmd() + ((user!=null) ? " cli:" + user + ";" : "");
    }

    public abstract String cmd();

    public abstract String outputFormat();
}
