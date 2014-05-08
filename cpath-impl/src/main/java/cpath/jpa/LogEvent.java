package cpath.jpa;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import org.springframework.util.Assert;

import cpath.config.CPathSettings;
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
	
	/**
	 * a special constant event type, because there is no DOWNLOAD command in the cpath2 api.
	 */
	public static LogEvent DOWNLOAD = new LogEvent(LogType.COMMAND, "DOWNLOAD");
	
	/**
	 * a special constant event type, because there is no IDMAPPING command in the cpath2 api.
	 */
	public static LogEvent IDMAPPING = new LogEvent(LogType.COMMAND, "IDMAPPING");
	
	/**
	 * a special constant event type for total access counts
	 */
	public static LogEvent TOTAL = new LogEvent(LogType.TOTAL, LogType.TOTAL.description);
	
	/**
	 * a special constant event type for other formats access counts (e.g., XML, JSON search, id-mapping query results)
	 */
	public static LogEvent FORMAT_OTHER = new LogEvent(LogType.FORMAT, "OTHER");
	
	
	public LogEvent() {
	}
	
	public LogEvent(LogType type, String name) {
		Assert.notNull(type);
		Assert.notNull(name);
		
		this.type = type;
		this.name = name;
	}
	
	
	public static LogEvent from(GraphType graphType) {	
		return new LogEvent(LogType.COMMAND, graphType.toString());
	}
	
	public static LogEvent from(Cmd command) {	
		return new LogEvent(LogType.COMMAND, command.toString());
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
	
	public static Set<LogEvent> fromProviders(Collection<String> providers) {
		Set<LogEvent> set = new HashSet<LogEvent>();
		for(String prov : providers)
			set.add(new LogEvent(LogType.PROVIDER, prov));
		return set;
	}
	
	
	/**
	 * Creates a list of things to update counts for -
	 * name, format, provider - from the 
	 * auto-generated data archive file.
	 * 
	 * @param filename see {@link CPathSettings#biopaxExportFileName(String)}
	 * 			for how it's created.
	 * @return
	 */
	public static Set<LogEvent> fromDownloads(String filename) {
		Set<LogEvent> set = new HashSet<LogEvent>();

		set.add(new LogEvent(LogType.FILE, filename));
		set.add(LogEvent.DOWNLOAD);
		
		// extract the orig. data source's standard name -
		// first, remove common prefix (incl. cPath2 instance name and ver.)
		if(filename.startsWith(CPathSettings.getInstance().exportArchivePrefix())) {
			int idx = CPathSettings.getInstance().exportArchivePrefix().length();
			String s = filename.substring(idx);
			String[] parts = s.split("\\.");
			assert parts.length > 1 : "split by '.' failed to produce " +
			"at least 2 parts from the filename: " + filename;
			//a hack: in order to skip for by-organism and special archives
			if(Character.isUpperCase(parts[0].charAt(0)) 
					&& !"All".equalsIgnoreCase(parts[0])
					&& !"Warehouse".equalsIgnoreCase(parts[0])) {
				set.add(new LogEvent(LogType.PROVIDER, parts[0]));
			}

			// extract the format
			OutputFormat format = null; 
			try { 
				format = OutputFormat.valueOf(parts[1]);
			} catch(Exception e) {

			}
			if(format != null)
				set.add(LogEvent.from(format));
			else {
				set.add(LogEvent.FORMAT_OTHER);
			}
		} else {
			set.add(LogEvent.FORMAT_OTHER);
		}
			
		
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
	
	
	@Override
	public String toString() {
		return type + " " + name;
	};
}
