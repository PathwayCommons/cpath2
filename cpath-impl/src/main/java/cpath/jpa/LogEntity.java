package cpath.jpa;

import org.springframework.util.Assert;


/**
 * @author rodche
 */
public class LogEntity extends AbstractEntity {
		
	private LogEvent event;
	
	private String addr;

	private String date;
	
	private Long count;

	/**
	 * 
	 * @param date ISO date (yyyy-MM-dd)
	 * @param event
	 * @param ipAddress
	 */
	public LogEntity(String date, LogEvent event, String ipAddress) {
		Assert.notNull(event);
		Assert.notNull(event.getType());
		Assert.notNull(event.getName());
		Assert.notNull(date);
		Assert.notNull(ipAddress);
		
		setEvent(event);
		setCount(0L);
		setDate(date);
			
		setAddr(ipAddress);
	}
	
	
	public String getAddr() {
		return addr;
	}
	public void setAddr(String addr) {
		this.addr = addr;
	}	
	
	public Long getCount() {
		return count;
	}
	public void setCount(Long count) {
		this.count = count;
	}

	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}

	public LogEvent getEvent() {
		return event;
	}
	public void setEvent(LogEvent event) {
		this.event = event;
	}
	
	@Override
	/**
	 * Mainly - for logging to the cpath2.log
	 */
	public String toString() {
		return String.format("%s, %s", addr, event);
	}
	
}
