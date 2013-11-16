/**
 * 
 */
package cpath.log.jpa;

import java.util.List;
import java.util.Map;

import cpath.log.LogType;


/**
 * @author rodche
 *
 */
interface LogEntitiesRepositoryCustom {
	
	/**
	 * Aggregates the history (log) of services/downloads provided 
	 * for users by service {@link LogType} and date, and 
	 * represents it as the list of [date, count] pairs for each category.
	 * 
	 * @return	log type - to array of Object[2] map, e.g.: 
	 * <pre>
	 * {
	 * "All":[["2013-11-14", 123], ["2013-11-15", 234],..], 
	 * "All Providers":[["2013-11-14", 23], ["2013-11-15", 34],..],  
	 * "All Files":[["2013-11-14", 23],..], 
	 * "All Commands":[["2013-11-14", 23],..],
	 * "All Errors":[["2013-11-14", 23],..],..
	 * }
	 * </pre>
	 */
	Map<String, List<Object[]>> downloadsTimeline(); 
	
		
	/**
	 * Aggregates the history (log) of a particular service category,
	 * {@link LogType}, provided for users by date; the result is a 
	 * list of [date, count] pairs for that category and service name.
	 * The result also includes the total counts by date for all names 
	 * in this category altogether (logType is used for the key).
	 * 
	 * @param logType	not null
	 * @param name
	 * @return	name (key) - to array of Object[2] map, 
	 * 			e.g., if logType eq. COMMANDS, and name were null: 
	 * <pre>
	 * {
	 * 	"All Commands": [["2013-11-13", 123], ["2013-11-14", 456],..], 
	 *	"search" : [["2013-11-13", 123],..], 
	 *	"get" : [["2013-11-13", 123],..], 
	 *	"traverse" : [["2013-11-13", 123],..],..
	 * }
	 * </pre>
	 * 
	 * If name, such as "search", were provided, then the result would
	 * be (other names not included): 
	 * <pre>
	 * {
	 * 	"All Commands": [["2013-11-13", 123], ["2013-11-14", 456],..], 
	 *	"search" : [["2013-11-13", 23], ["2013-11-14", 45],..]
	 * }
	 * </pre> 
	 */
	Map<String, List<Object[]>> downloadsTimeline(LogType logType, String name); 
	
	
	
	//TODO add log summary by geoloc methods
	
	
	
	void log(String date, LogType logType, String name, String ipAddress);
}
