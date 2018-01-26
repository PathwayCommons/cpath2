package cpath.service.args;

/**
 * Created by igor on 23/06/16.
 */
public abstract class ArgsBase {
    private String user;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public abstract String getLabel();
}
