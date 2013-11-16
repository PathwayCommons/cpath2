/**
 * 
 */
package cpath.log;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.maxmind.geoip.Country;
import com.maxmind.geoip.LookupService;

import cpath.log.jpa.Geoloc;
import cpath.log.jpa.LogEntitiesRepository;
import cpath.log.jpa.LogEntity;

/**
 * @author rodche
 *
 */
public final class LogUtils {
	
	// GeoLite IP location (country) lookup service
	static LookupService geoliteCountry; 
	//TODO might add the GeoLite city DB (it's much larger)
	
	public static final DateFormat ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	
	static {
		//GeoIP.dat must be present at the classpath root (see the pom.xml, use "mvn download:wget" and gunzip)
		String dbfile = LogUtils.class.getResource("/GeoIP.dat").getPath();
//		System.out.println("GeoLite: reading the database from " + dbfile);
		try {
			geoliteCountry = new LookupService(dbfile, LookupService.GEOIP_MEMORY_CACHE);
		} catch (IOException e) {
			throw new RuntimeException("Fauled initializing GeoLite LookupService", e);
		}
	}
	
	
	protected LogUtils() {
		throw new AssertionError("Not instantiable");
	}

	
	/**
	 * Increases the number (counter) 
	 * of today user's requests of some 
	 * sort and location.
	 * 
	 * Right now, only country code (resolved from the IP address) 
	 * matters for location matching (city and region are ignored).
	 * 
	 * @param repository
	 * @param date 
	 * @return
	 */
	public static LogEntity count(LogEntitiesRepository repository, String date, 
			LogType type, String name, String ipAddress) {
		
		Geoloc loc = Geoloc.fromIpAddress(ipAddress);		
		
		// TODO only country code matters in the location right now (change later?)
		// (clear 'city' and 'region' for now, if they were set)
		loc.setRegion(null);
		loc.setCity(null);
		
		// find or create a record, count+1
		LogEntity t = (LogEntity) repository.findByTypeAndNameAndGeolocCountryAndDate(
				type, name, loc.getCountry(), date);
		if(t == null) {			
			t = new LogEntity(type, name, date, loc);
		}
		
		t.setCount(t.getCount() + 1);
		
		return repository.save(t);
	}
	
	/**
	 * Gets the country code by IP 
	 * (region, city is empty).
	 * 
	 * @param ipAddress
	 * @return
	 */
	public static Geoloc countryLookup(String ipAddress) {
		Country country = geoliteCountry.getCountry(ipAddress);
		return new Geoloc(country.getCode(), country.getName(), null, null);
	}
	
	
	public static String today() {
		return ISO_DATE_FORMAT.format(new Date());
	}
	
	public static String isoDate(Date date) {
		return ISO_DATE_FORMAT.format(date);
	}
	
	/**
	 * Get a new date (ISO) by adding or 
	 * subtracting (if days < 0) the number 
	 * of days to/from specified date.
	 * 
	 * @param date
	 * @param days
	 * @return
	 */
	public static String addIsoDate(Date date, int days) {
		Calendar cal = Calendar.getInstance();    
		cal.setTime(date);    
		cal.add( Calendar.DATE, days );    
		return ISO_DATE_FORMAT.format(cal.getTime()); 
	}
	
	
	/**
	 * Get a new date (ISO) by adding or 
	 * subtracting (if days < 0) the number 
	 * of days to/from specified date.
	 * 
	 * @param date
	 * @param days
	 * @return
	 */
	public static String addIsoDate(String isoDate, int days) {
		Date date;
		try {
			date = ISO_DATE_FORMAT.parse(isoDate);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
		return addIsoDate(date, days);
	}
	
}
