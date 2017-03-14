package cpath.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.util.Assert;

/**
 * A pair of type and name that describes a service access/use event.
 * A list of such events may be associated with a client IP address
 * and date, and used to record (log), e.g., which api command,
 * data format, data sources, filename, etc., were accessed in one's web request.
 * 
 * @author rodche
 */
public class LogEvent {

	private LogType type;
	private String name;
	
	/**
	 * Constructor.
	 * @param type
	 * @param name is always turned to lower case
	 */
	private LogEvent(LogType type, String name) {
		Assert.notNull(type);
		Assert.notNull(name);
		setName(name);
		setType(type);
	}
	
	public static LogEvent kind(GraphType graphType) {
		return new LogEvent(LogType.COMMAND, graphType.toString());
	}
	
	public static LogEvent command(Cmd command) {
		return new LogEvent(LogType.COMMAND, command.toString());
	}
	
	public static LogEvent format(OutputFormat outputFormat) {
		return new LogEvent(LogType.FORMAT, outputFormat.toString());
	}
		
	public static LogEvent error(Status status) {
		return new LogEvent(LogType.ERROR, status.name());
	}
	
	public static LogEvent error(ErrorResponse errorResponse) {
		return error(errorResponse.getStatus());
	}

	public static LogEvent client(String client) {
		return new LogEvent(LogType.CLIENT, client.toLowerCase());
	}

	public static LogEvent provider(String datasource) {
		return new LogEvent(LogType.PROVIDER, datasource.toLowerCase());
	}

	public static Set<LogEvent> providers(Collection<String> providers) {
		Set<LogEvent> set = new HashSet<LogEvent>();
		if(providers != null) {
			for(String prov : providers) {
				set.add(LogEvent.provider(prov));
			}
		}
		return set;
	}

	public LogType getType() {
		return type;
	}

	public String getName() {//return copy
		return name.toLowerCase();
	} //toLowerCase will fix for old names in the h2.db

	public void setType(LogType type) {
		this.type = type;
	}

	public void setName(String name) {
		this.name = name.toLowerCase();
	}


	@Override
	public String toString() {
		return String.format("type:%s, name:'%s'", type, name);
	}

	/**
     * Categories of the data download/access events.
     *
     * In fact, these are not mutually
     * exclusive types, for a web request/response usually
     * counts more than once: once, for sure, in the "total"
     * category but might also - in "providers", "commands",
     * "formats", or "errors" at the same time.
     *
     * @author rodche
     *
     */
    public static enum LogType {
        PROVIDER("All Providers"),
        COMMAND("All Web Commands"),
        FORMAT("All Output Formats"),
        ERROR("All Errors"),
		CLIENT("All Clients"),
		;

        public final String description;

        LogType(String description) {
            this.description = description;
        }
    }
}
