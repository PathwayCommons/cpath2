package cpath.log.jpa;


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
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"date", "name", "country", "region"}))
public class LogEntity extends AbstractEntity {
		
	@Embedded
	@AttributeOverrides({
	    @AttributeOverride(name="type", column=@Column(name="type")),
	    @AttributeOverride(name="name", column=@Column(name="name"))
	})
	private LogEvent event;
	
	@Embedded
	@AttributeOverrides({
	    @AttributeOverride(name="country", column=@Column(name="country")),
	    @AttributeOverride(name="region", column=@Column(name="region")),
	})
	private Geoloc geoloc;

	@Column(nullable=false)
	private String date;
	
	private Long count;	
	
	public LogEntity() {
	}
	
	/**
	 * 
	 * @param date ISO date (yyyy-MM-dd)
	 * @param event
	 * @param geoloc
	 */
	public LogEntity(String date, LogEvent event, Geoloc geoloc) {
		Assert.notNull(event);
		Assert.notNull(event.getType());
		Assert.notNull(event.getName());
		Assert.notNull(date);
		Assert.notNull(geoloc);
		setEvent(event);
		setCount(0L);
		setDate(date);
		setGeoloc(geoloc);
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
