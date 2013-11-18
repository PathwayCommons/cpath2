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
//	private String region; //if empty - all regions
//	private String city; //if empty - all cities
	private String countryName; //e.g., "Canada"
	
	public Geoloc() {
	}
	
//	public Geoloc(String country, String countryName, String region, String city) {
	public Geoloc(String country, String countryName, String region, String city) {
		Assert.notNull(country);
		Assert.notNull(countryName);
		
		this.country = country;
		this.countryName = countryName;
//		this.region = region;
//		this.city = city;
	}
	
	public String getCountry() {
		return country;
	}
	public void setCountry(String country) {
		this.country = country;
	}
	
	public String getCountryName() {
		return countryName;
	}
	public void setCountryName(String countryName) {
		this.countryName = countryName;
	}

//	public String getRegion() {
//		return region;
//	}
//	public void setRegion(String region) {
//		this.region = region;
//	}
//	public String getCity() {
//		return city;
//	}
//	public void setCity(String city) {
//		this.city = city;
//	}
		
	public static Geoloc fromIpAddress(String ipAddress) {
		return LogUtils.lookup(ipAddress);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
//		if(city != null)
//			sb.append(city).append(" ");
//		
//		if(region != null)
//			sb.append(region).append(" ");
		
		sb.append(countryName).append(" (").append(country).append(")");
		
		return sb.toString();
	}
	
}
