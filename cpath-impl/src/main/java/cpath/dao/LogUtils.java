package cpath.dao;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;

import cpath.config.CPathSettings;
import cpath.jpa.Geoloc;

/**
 * @author rodche
 *
 */
public final class LogUtils {
	
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
	
	static String isoDate(Date date) {
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
	static String addIsoDate(Date date, int days) {
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
