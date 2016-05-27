package cpath.service;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author rodche
 *
 */
public final class LogUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(LogUtils.class);

	public static final DateFormat ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

	protected LogUtils() {
		throw new AssertionError("Not instantiable");
	}

	public static String yesterday() {
		Calendar cal = Calendar.getInstance();
	    cal.add(Calendar.DATE, -1);
		Date yesterday = cal.getTime();
		return ISO_DATE_FORMAT.format(yesterday);
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
	 * @param isoDate
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
