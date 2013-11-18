package cpath.log.jpa;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import org.springframework.util.Assert;

import cpath.log.LogType;
import cpath.service.Cmd;
import cpath.service.ErrorResponse;
import cpath.service.GraphType;
import cpath.service.OutputFormat;
import cpath.service.Status;

/**
 * A pair of type and name that describes log entry.
 * 
 * A convenience bean for logging
 * a server access event from different 
 * perspectives. A list of such value 
 * pairs can be associated with a request IP address
 * and date and used to increment corresponding access counts 
 * all at once, e.g., to simultaneously log which data sources
 * were used, format, command, filename, etc., from one web request.
 * 
 * @see LogEntity
 * 
 * @author rodche
 *
 */
@Embeddable
public class LogEvent {
	
	@Column(nullable=false)
	@Enumerated(EnumType.STRING)
	private LogType type;

	@Column(nullable=false)
	private String name;
	
	
	public LogEvent() {
	}
	
	public LogEvent(LogType type, String name) {
		Assert.notNull(type);
		Assert.notNull(name);
		
		this.type = type;
		this.name = name;
	}
	
	
	public static LogEvent from(Cmd command, GraphType graphType) {	
		if(command != Cmd.GRAPH)
			return new LogEvent(LogType.COMMAND, command.toString());
		else {
			return new LogEvent(LogType.COMMAND, graphType.toString());
		}
	}
	
	public static LogEvent from(OutputFormat outputFormat) {
		return new LogEvent(LogType.FORMAT, outputFormat.toString());
	}
	
	
	/*for tests*/
	static LogEvent fromStatusCode(int statusCode) {
		return new LogEvent(LogType.ERROR, String.valueOf(statusCode));
	}
		
	public static LogEvent from(Status status) {
		return new LogEvent(LogType.ERROR, status.name());
	}
	
	public static LogEvent from(ErrorResponse errorResponse) {
		return from(errorResponse.getStatus());
	}
	
	public static Set<LogEvent> fromProviders(Set<String> providers) {
		Set<LogEvent> set = new HashSet<LogEvent>();
		for(String prov : providers)
			set.add(new LogEvent(LogType.PROVIDER, prov));
		return set;
	}
	
	public static LogEvent download() {
		return new LogEvent(LogType.COMMAND, "DOWNLOAD");
	}
	
	public static LogEvent total() {
		return new LogEvent(LogType.TOTAL, LogType.TOTAL.description);
	}
	
	public static Set<LogEvent> fromDownloadsArchive(String filename) {
		Set<LogEvent> set = new HashSet<LogEvent>();
		
		//TODO extract name, format, provider from cPath2 archive name
		
		return set;
	}
	

	public LogType getType() {
		return type;
	}

	public String getName() {//return copy
		return name;
	}

	public void setType(LogType type) {
		this.type = type;
	}

	public void setName(String name) {
		this.name = name;
	}
	
}
