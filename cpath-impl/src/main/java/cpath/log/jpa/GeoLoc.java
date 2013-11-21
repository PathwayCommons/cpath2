/**
 * 
 */
package cpath.log.jpa;


import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.springframework.util.Assert;

import cpath.log.LogUtils;


/**
 * @author rodche
 *
 */
@Embeddable
public class Geoloc {

	@Column(nullable=false)
	private String country; //country code, e.g., "US", "CA", etc.
	@Column(nullable=true)
	private String region; //if empty - all regions
	
	public Geoloc() {
	}
	
	public Geoloc(String country, String region) {
		Assert.notNull(country);
		
		this.country = country;
		this.region = region;
	}
	
	public String getCountry() {
		return country;
	}
	public void setCountry(String country) {
		this.country = country;
	}
	
	public String getRegion() {
		return region;
	}
	public void setRegion(String region) {
		this.region = region;
	}
		
	public static Geoloc fromIpAddress(String ipAddress) {
		return LogUtils.lookup(ipAddress);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		if(region != null)
			sb.append(region).append(" ");
		
		sb.append(country);
		
		return sb.toString();
	}
	
}
