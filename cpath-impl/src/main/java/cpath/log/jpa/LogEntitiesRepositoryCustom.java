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
	 * @param e.g, provider's name, command, format, or filename (depends on the log type)
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
	 * If name, e.g. "search", were provided, the result would be: 
	 * <pre>
	 * {
	 * 	"All Commands": [["2013-11-13", 123], ["2013-11-14", 456],..], 
	 *	"search" : [["2013-11-13", 23], ["2013-11-14", 45],..]
	 * }
	 * </pre> 
	 */
	Map<String, List<Object[]>> downloadsTimeline(LogType logType, String name); 
	
		
	/**
	 * The number of client requests served, grouped by country code.
	 * The result is a list of [country, count] optionally filtered 
	 * by the service type and name if the arguments were provided, 
	 * as follows:
	 * <ul>
	 * <li>neither type nor name - total counts;</li>
	 * <li>only name is provided - counts for this name 
	 *		(ignoring type, which usually implies one particular type, 
	 *		because a name cannot belong to different types...);</li>
	 * <li>type, except TOTAL, and name -> counts for all events of 
	 * 		the name and type (in fact, it does not depend on the type; hence same as above).</li>
	 * <li>type, except TOTAL, but no name -> counts for all events of the type;</li>
	 * </ul>
	 * 
	 * 
	 * @param logType filter by (limit to one) category, e.g., provider, file, command, or total, etc.
	 * @param name filter by name (e.g, provider's name, command, format, or filename)
	 * @return	array of Object[2] items: 
	 * <pre>
	 * {[['Country', 'Downloads'], ['US', 123], ['CA', 456], ['RU', 45],..]}
	 * </pre> 
	 */
	List<Object[]> downloadsCountry(LogType logType, String name);
	
	
	/**
	 * The number of client requests served, grouped by location.
	 * The result is a list of [country, region, count] optionally 
	 * filtered by the service type and name, if the arguments 
	 * were provided, as follows:
	 * <ul>
	 * <li>neither type nor name - total counts;</li>
	 * <li>only name is provided - counts for this name 
	 *		(ignoring type, which usually implies one particular type, 
	 *		because a name cannot belong to different types...);</li>
	 * <li>type, except TOTAL, and name -> counts for all events of 
	 * 		the name and type (in fact, it does not depend on the type; hence same as above).</li>
	 * <li>type, except TOTAL, but no name -> counts for all events of the type;</li>
	 * </ul>
	 * 
	 * @param logType
	 * @param name
	 * @return
	 * <pre>
	 * {[['Country', 'Region', 'Downloads'], ['US', 'California', 33],..]}
	 * </pre>  
	 */
	List<Object[]> downloadsGeography(LogType logType, String name);
	
}
