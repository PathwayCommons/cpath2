/**
 * 
 */
package cpath.dao;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.util.Assert;

import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;

import cpath.config.CPathSettings;
import cpath.jpa.Geoloc;
import cpath.jpa.LogEntitiesRepository;
import cpath.jpa.LogEntity;
import cpath.jpa.LogEvent;
import cpath.jpa.LogType;

/**
 * @author rodche
 *
 */
public final class LogUtils {
	
	private static final Logger LOG = LoggerFactory.getLogger(LogUtils.class);
	
	static LookupService geolite; 
	
	public static final DateFormat ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	
	static {
		//will be downloaded if not exists already (one can delete the file to auto-update)
		String localFileName = CPathSettings.getInstance().homeDir() + File.separator + "GeoLiteCity.dat";
		try {
			CPathUtils.download(
				"http://geolite.maxmind.com/download/geoip/database/GeoLiteCity.dat.gz", 
					localFileName, true, false); // - also gunzip			
			geolite = new LookupService(
				localFileName,
				LookupService.GEOIP_MEMORY_CACHE
			);
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
		
		if(loc == null)
			loc = new Geoloc();
		
		// find or create a record, count+1
		LogEntity t = null;
		try {
			t = (LogEntity) repository.findByEventNameAndGeolocCountryAndGeolocRegionAndGeolocCityAndDate(
				event.getName(), loc.getCountry(), loc.getRegion(), loc.getCity(), date);
		} catch (DataAccessException e) {
			LOG.error("count(), findByEventNameAndGeolocCountryAndGeolocRegionAndGeolocCityAndDat " +
				"failed to update for event: " + event.getName() + 
				", loc: " + loc.toString() + ", date: " + date, e);
		}
		
		if(t == null) {			
			t = new LogEntity(date, event, loc);
		}
		
		t.setCount(t.getCount() + 1);
		
		return repository.save(t);
	}	
	
	
	/**
	 * Gets a geographical location by IP address
	 * using the GeoLite database.
	 * 
	 * @param ipAddress
	 * @return location or null (when it cannot be found; e.g., if it's local IP)
	 */
	public static Geoloc lookup(String ipAddress) {
		Location geoloc = geolite.getLocation(ipAddress);
		return (geoloc != null) 
			? new Geoloc(geoloc.countryCode, geoloc.region, geoloc.city) 
				: null;
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
		
		//total counts (is not sum of the above); counts once per request/response
		count(repository, day, LogEvent.TOTAL, loc);
		
	}
}
