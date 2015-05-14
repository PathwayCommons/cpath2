/**
 * 
 */
package cpath.jpa;

import java.util.List;
import java.util.Map;



/**
 * @author rodche
 *
 */
interface LogEntitiesRepositoryCustom {
	
	/**
	 * Analyzes the downloads/access history (log events) and 
	 * aggregates the access counts in the specified service category,
	 * {@link LogType}, given event name (or all), on each day; 
	 * the result is a list of [date, count] pairs for the name(s) in the category.
	 * 
	 * @param logType	not null
	 * @param e.g, provider's name, command, format, or filename (depends on the log type)
	 * @return	name (key) - to array of Object[2] map, 
	 * 			e.g., if logType eq. COMMAND, and name were null: 
	 * <pre>
	 * {
	 *	"search" : [["2013-11-13", 123],..], 
	 *	"get" : [["2013-11-13", 123],..], 
	 *	"traverse" : [["2013-11-13", 123],..],..
	 * }
	 * </pre>
	 * 
	 * If name, e.g. "search", were provided, the result would be: 
	 * <pre>
	 * {
	 *	"search" : [["2013-11-13", 23], ["2013-11-14", 45],..]
	 * }
	 * </pre> 
	 */
	Map<String, List<Object[]>> downloadsTimeline(LogType logType, String name); 
	
	/**
	 * Aggregates the number of downloads/access (log events) 
	 * in a particular category, {@link LogType}, from the first day to each day; 
	 * the result is a list of [date, count] pairs for the name(s) in the category.
	 * 
	 * @param logType	not null
	 * @param e.g, provider's name, command, format, or filename (depends on the log type)
	 * @return	name (key) - to array of Object[2] map, 
	 * 			e.g., if logType eq. COMMAND, and name were null: 
	 * <pre>
	 * {
	 *	"search" : [["2013-11-13", 723],..], 
	 *	"get" : [["2013-11-13", 323],..],...
	 * }
	 * </pre>
	 * 
	 * If name, e.g. "search", were provided, the result would be: 
	 * <pre>
	 * {
	 *	"search" : [["2013-11-13", 723], ["2013-11-14", 945],..]
	 * }
	 * </pre> 
	 */
	Map<String, List<Object[]>> downloadsTimelineCum(LogType logType, String name); 	

	/**
	 * Gets the daily numbers of unique client IP addresses 
	 * per service request type (or category),
	 * {@link LogType}. The result is a list of [date, count] 
	 * pairs for the name(s) in the category.
	 * 
	 * @param logType	not null
	 * @param e.g, provider's name, command, format, or filename (depends on the log type)
	 * @return	name (key) - to array of Object[2] map, 
	 * 			e.g., if logType eq. COMMANDS, and name were null: 
	 * <pre>
	 * {
	 *	"search" : [["2013-11-13", 123],..], 
	 *	"get" : [["2013-11-13", 123],..], 
	 *	"traverse" : [["2013-11-13", 123],..],..
	 * }
	 * </pre>
	 * 
	 * If name, e.g. "search", were provided, the result would be: 
	 * <pre>
	 * {
	 *	"search" : [["2013-11-13", 23], ["2013-11-14", 45],..]
	 * }
	 * </pre> 
	 */
	Map<String, List<Object[]>> ipsTimeline(LogType logType, String name); 
	
	
	/**
	 * Gets the numbers of unique client IP addresses 
	 * collected from the very first log event to each date, per
	 * service request type (category),
	 * {@link LogType} and name (optional). 
	 * The result is a list of [date, count] 
	 * pairs for the name(s) in the category.
	 * 
	 * @param logType	not null
	 * @param e.g, provider's name, command, format, or filename (depends on the log type)
	 * @return	name (key) - to array of Object[2] map, 
	 * 			e.g., if logType eq. COMMANDS, and name were null: 
	 * <pre>
	 * {
	 *	"search" : [["2013-11-13", 123],..], 
	 *	"get" : [["2013-11-13", 123],..], 
	 *	"traverse" : [["2013-11-13", 123],..],..
	 * }
	 * </pre>
	 * 
	 * If name, e.g. "search", were provided, the result would be: 
	 * <pre>
	 * {
	 *	"search" : [["2013-11-13", 23], ["2013-11-14", 45],..]
	 * }
	 * </pre> 
	 */
	Map<String, List<Object[]>> ipsTimelineCum(LogType logType, String name); 	
		
	
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
	List<Object[]> downloadsWorld(LogType logType, String name);

	/**
	 * The number of client requests served, grouped by city,
	 * filtered by country.
	 * 
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
	 * @param countryCode not null
	 * @param logType filter by (limit to one) category, e.g., provider, file, command, or total, etc.
	 * @param name filter by name (e.g, provider's name, command, format, or filename)
	 * @return	array of Object[2] items: 
	 * <pre>
	 * {[['City', 'Downloads'], ['Mountain View', 23], ['Los Angeles', 56],..]}
	 * </pre> 
	 */
	List<Object[]> downloadsCountry(String countryCode, LogType logType, String name);	
	
	/**
	 * The number of client requests served, grouped by location.
	 * The result is a list of [country, region, city, count] 
	 * optionally filtered by the service type and name, 
	 * if the arguments were provided, as follows:
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
	 * {[['Country', 'Region', 'City', 'Downloads'], ['US', 'CA', 'Mountain View', 33],..]}
	 * </pre>  
	 */
	List<Object[]> downloadsGeography(LogType logType, String name);
	
	/**
	 * All distinct events logged so far (unique type+name).
	 * 
	 * @param logType
	 * @return
	 */
	List<LogEvent> logEvents(LogType logType);

	
	/**
	 * The total number of client requests served, 
	 * filtered by the service name.
	 * 
	 * @param name log event name (a command, provider name, filename, error name, etc.)
	 * @return
	 */
	Long downloads(String name);
	
	
	/**
	 * The number of unique client IP addresses, 
	 * filtered by the service name.
	 * 
	 * @param name log event name (a command, provider name, filename, error name, etc.)
	 * @return
	 */
	Long uniqueIps(String name);

	
	/**
	 * List of unique client IP addresses,
	 * where the requests of the specified category 
	 * and/or name came from.
	 * 
	 * @param logType
	 * @return
	 */
	List<String> listUniqueIps(LogType logType, String name);

	
	/**
	 * The total number of requests. 
	 * 
	 * @return
	 */
	Long totalRequests();

	
	/**
	 * The total number of service/data errors 
	 * of any sort.
	 * 
	 * @return
	 */
	Long totalErrors();	
	
	
	/**
	 * The total number of unique clients 
	 * (IP addresses).
	 * 
	 * @return
	 */
	Long totalUniqueIps();
	
}
