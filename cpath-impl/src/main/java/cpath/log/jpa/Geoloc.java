/**
 * 
 */
package cpath.log.jpa;


import javax.persistence.Embeddable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import cpath.log.LogUtils;


/**
 * @author rodche
 *
 */
@Embeddable
public class Geoloc {
	
	private static final Logger LOG = LoggerFactory.getLogger(Geoloc.class);

	private String country; //country code, e.g., "US", "CA", etc.
	private String region;
	private String city;
	
	public Geoloc() {
	}
	
	public Geoloc(String country, String region, String city) {
		Assert.hasText(country);
		Assert.hasText(region);
		Assert.hasText(city);
		this.country = country;
		this.region = region;
		this.city = city;
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
	
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}

	/**
	 * Creates a new location bean from the IP address.
	 * 
	 * @param ipAddress
	 * @return location or null (if IP was a LAN one or not IPv4)
	 */
	public static Geoloc fromIpAddress(String ipAddress) {
		Geoloc loc = LogUtils.lookup(ipAddress);
		
		if(loc == null)
			LOG.debug("Unknown geo location, IP: " + ipAddress);
		
		return loc;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		if(city != null)
			sb.append(city).append(" ");
		
		if(region != null)
			sb.append(region).append(" ");
		
		if(country != null)
			sb.append(country);
		
		return sb.toString();
	}
	
}
