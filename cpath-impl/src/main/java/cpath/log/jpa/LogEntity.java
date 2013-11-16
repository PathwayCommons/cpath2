package cpath.log.jpa;


import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.util.Assert;

import cpath.log.LogType;


/**
 * @author rodche
 *
 */
@Entity
@DynamicUpdate
@DynamicInsert
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"date", "type", "name", "geoloc_id"}))
public class LogEntity extends AbstractEntity {
	
	@Column(nullable=false)
	@Enumerated(EnumType.STRING)
	private LogType type;
	
	private String name;
	private Long count;
	
	@Column(nullable=false)
	private String date;
	
	@ManyToOne(cascade=CascadeType.ALL)
	@JoinColumn(name="geoloc_id")
	private Geoloc geoloc;
	
	public LogEntity() {
	}
	
	/**
	 * 
	 * @param type of the log entry
	 * @param name e.g., web command or data provider's name
	 * @param date ISO date (yyyy-MM-dd)
	 * @param geoloc
	 */
	public LogEntity(LogType type, String name, String date, Geoloc geoloc) {
		Assert.notNull(type);
//		Assert.notNull(name); //can be null - means "all" (of the type)
		Assert.notNull(date);
		Assert.notNull(geoloc);
		setType(type);
		setName(name);
		setCount(0L);
		setDate(date);
		setGeoloc(geoloc);
	}
	
	
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}	
	
	public Long getCount() {
		return count;
	}
	public void setCount(Long count) {
		this.count = count;
	}
	
	public LogType getType() {
		return type;
	}
	public void setType(LogType type) {
		this.type = type;
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

}
