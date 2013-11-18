/**
 * 
 */
package cpath.log;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import org.springframework.util.Assert;

import com.maxmind.geoip.Country;
import com.maxmind.geoip.LookupService;

import cpath.log.jpa.Geoloc;
import cpath.log.jpa.LogEntitiesRepository;
import cpath.log.jpa.LogEntity;
import cpath.log.jpa.LogEvent;

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

	
	/*
	 * Increases the number (counter) 
	 * of today user's requests of some 
	 * sort and location (IP address's country).
	 */
	static LogEntity count(LogEntitiesRepository repository, String date, 
			LogEvent event, String ipAddress) {
		
		Geoloc loc = Geoloc.fromIpAddress(ipAddress);
		
		return count(repository, date, event, loc);
	}

	
	/**
	 * Increases the number (counter) 
	 * of today user's requests of some 
	 * sort and location.
	 * 
	 * Right now, only country code there
	 * matters for location matching 
	 * (city and region are ignored).
	 * 
	 * @param repository
	 * @param date
	 * @param event
	 * @param loc only country is there used, and region, city - ignored.
	 * @return
	 */
	static LogEntity count(LogEntitiesRepository repository, 
			String date, LogEvent event, Geoloc loc) {
		
		// TODO only country code matters right now (change later?)
//		assert loc.getRegion()==null && loc.getCity()==null : "loc.region or loc.city is not null";
//		loc.setRegion(null);
//		loc.setCity(null);		
		
		// find or create a record, count+1
		LogEntity t = (LogEntity) repository.findByEventTypeAndEventNameAndGeolocCountryAndDate(
				event.getType(), event.getName(), loc.getCountry(), date);
		if(t == null) {			
			t = new LogEntity(date, event, loc);
		}
		
		t.setCount(t.getCount() + 1);
		
		return repository.save(t);
	}	
	
	
	/**
	 * Gets location by IP address 
	 * (currently, gets only country code and name; 
	 * region, city will be null).
	 * 
	 * @param ipAddress
	 * @return
	 */
	public static Geoloc lookup(String ipAddress) {
		Country country = geoliteCountry.getCountry(ipAddress);
		Geoloc loc = new Geoloc(country.getCode(), country.getName(), null, null);
		return loc;
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
	
		
	/**
	 * Saves and counts a series of data access events 
	 * (usually associated with the same web request) 
	 * to the log db.
	 * 
	 * @param repository
	 * @param events
	 * @param loc
	 */
	public static void log(LogEntitiesRepository repository, Collection<LogEvent> events, Geoloc loc) {
		log(repository, today(), events, loc);
	}
	
	
	/*
	 * package private - for tests
	 */
	static void log(LogEntitiesRepository repository, String day, Collection<LogEvent> events, Geoloc loc) {
		
		for(LogEvent event : events) {
			//'total' should not be there (auto-counts once anyway!)
			Assert.isTrue(event.getType() != LogType.TOTAL); 
			
			count(repository, day, event, loc);
		}
		
		count(repository, day, LogEvent.total(), loc);
		
	}
}
