package cpath.service;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author rodche
 *
 */
public final class LogUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(LogUtils.class);
	
	public static final DateFormat ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

	//these regexp are to match current cpath2 version archives names only
	final public static Pattern ARCHIVE_SRC_PATTERN = Pattern.compile("^\\S+?\\.(.+?)\\..+\\.gz$");
	final public static Pattern ARCHIVE_FORMAT_PATTERN = Pattern.compile("^\\S+?\\..+?\\.(.+?)\\..+\\.gz$");
	
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

	/**
	 * Detects the format of a cpath2 archive 
	 * (from the downloads directory)
	 * 
	 * @param archiveFilename
	 * @return output format, or null if not recognized
	 */
	public static OutputFormat fileOutputFormat(String archiveFilename) {
		OutputFormat of = null;
		Matcher m = ARCHIVE_FORMAT_PATTERN.matcher(archiveFilename);
		if(m.matches()) {
			try {
				of = OutputFormat.valueOf(m.group(1).toUpperCase());
			} catch(IllegalArgumentException e) {
				LOGGER.error("Unknown FORMAT value '" + m.group(1) 
						+ "' in auto-generated " + archiveFilename + " (ignore if it's a test)");
			}
		}
		return of;
	}
	
	/**
	 * For a a cpath2 archive file in the downloads directory,
	 * gets the part of its name that describes data scope that is
	 * either:
	 * - datasource identifier or standard name (older cpath2 versions);
	 * - organism name;
	 * - or special sub-model name, such as 'All', 'Detailed', 'Warehouse'
	 *
	 * @param archiveFilename
	 * @return name or null if the filename did not match the archive naming pattern.
	 */
	public static String fileSrcOrScope(String archiveFilename) {
		String src = null;
		Matcher m = ARCHIVE_SRC_PATTERN.matcher(archiveFilename);
		if(m.matches()) {
			src = m.group(1);
		}
		return src;
	}
}
