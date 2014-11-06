package cpath.jpa;


import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.util.Assert;



/**
 * @author rodche
 *
 */
@Entity
@DynamicUpdate
@DynamicInsert
//uniqueConstraints: 'type' is not listed because 'name' is enough (should never use same log name with different types)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"date", "name", "addr"}))
public class LogEntity extends AbstractEntity {
		
	@Embedded
	@AttributeOverrides({
	    @AttributeOverride(name="type", column=@Column(name="type")),
	    @AttributeOverride(name="name", column=@Column(name="name"))
	})
	private LogEvent event;
	
	@Column
	private String addr;
	
	@Embedded
	@AttributeOverrides({
	    @AttributeOverride(name="country", column=@Column(name="country")),
	    @AttributeOverride(name="region", column=@Column(name="region")),
	    @AttributeOverride(name="city", column=@Column(name="city"))
	})
	private Geoloc geoloc;

	@Column(nullable=false)
	private String date;
	
	@Column(nullable=false)
	private Long count;	
	
	public LogEntity() {
	}
	
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
		geoloc = Geoloc.fromIpAddress(ipAddress);
		setGeoloc(geoloc);
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
	
	public Geoloc getGeoloc() {
		return geoloc;
	}
	public void setGeoloc(Geoloc geoloc) {
		this.geoloc = geoloc;
	}

	public LogEvent getEvent() {
		return event;
	}
	public void setEvent(LogEvent event) {
		this.event = event;
	}
	
}
