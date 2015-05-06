/**
 * 
 */
package cpath.jpa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.support.QueryDslRepositorySupport;
import org.springframework.util.Assert;

import com.mysema.query.Tuple;
import com.mysema.query.jpa.JPQLQuery;

import cpath.dao.LogUtils;
import cpath.jpa.QLogEntity;
import cpath.service.CPathService;

/**
 * @author rodche
 *
 */
class LogEntitiesRepositoryImpl extends QueryDslRepositorySupport 
	implements LogEntitiesRepositoryCustom {
	
	public LogEntitiesRepositoryImpl() {
		super(LogEntity.class);
	}
	
	@Autowired
	CPathService service;
	
	@Override
	public Map<String, List<Object[]>> downloadsTimeline(LogType logType, String name) 
	{
		Map<String, List<Object[]>> timeline = new TreeMap<String, List<Object[]>>();			
		QLogEntity $ = QLogEntity.logEntity;
		
		if(name == null || name.isEmpty()) {
			Assert.notNull(logType);
			for(Tuple t : from($)
					.where($.event.type.eq(logType), 
							//and IP is either null (records before 11/2014) or real IP (not the reserved one)
							$.addr.isNull().or($.addr.ne(LogUtils.UNIQUE_IP)))
					.groupBy($.event.name,$.date)
					.orderBy($.event.name.asc(),$.date.desc())
					.list($.event.name,$.date,$.count.sum())) 
			{
				String key = t.get($.event.name);
				List<Object[]> val = timeline.get(key);
				if(val == null) {
					val = new ArrayList<Object[]>();
					timeline.put(key, val);
				}
				val.add(new Object[] {t.get($.date), t.get($.count.sum())});
			}
		} 
		else { //name was provided; type does not matter anymore
			List<Object[]> val = new ArrayList<Object[]>();
			timeline.put(name, val);			
			for(Tuple t : from($)
					.where($.event.name.eq(name), 
							//and IP is either null (records before 11/2014) or real IP (not the reserved one)
							$.addr.isNull().or($.addr.ne(LogUtils.UNIQUE_IP)))
					.groupBy($.date).orderBy($.date.desc())
					.list($.date,$.count.sum())) {
				val.add(new Object[] {t.get($.date), t.get($.count.sum())});
			}		
		}
		
		return timeline;
	}

	
	@Override
	public List<Object[]> downloadsWorld(LogType logType, String name) {
		
		List<Object[]> list = new ArrayList<Object[]>();		
		QLogEntity $ = QLogEntity.logEntity;
		
		JPQLQuery query = from($)
				.where(//IP is either null (records before 11/2014) or real IP (not the reserved one)
					$.addr.isNull().or($.addr.ne(LogUtils.UNIQUE_IP)))
				.groupBy($.geoloc.country)
				.orderBy($.geoloc.country.asc());
		
		//neither type nor name -> total counts by country
		if((logType == null || logType == LogType.TOTAL) && (name == null || name.isEmpty())) {
			query.where($.event.type.eq(LogType.TOTAL));
		} else if((logType == null || logType == LogType.TOTAL) && name != null && !name.isEmpty()) {
			//only name is provided -> counts by country for this name (ignore type)
			query.where($.event.name.eq(name));
		} else if(name == null || name.isEmpty()) {
			// type, except TOTAL, but no name -> counts by country for all events of the type
			query.where($.event.type.eq(logType));
		} else { // type, except TOTAL, and name -> counts by country for all events of the type, name
			query.where($.event.type.eq(logType), $.event.name.eq(name));
		}
		
		for(Tuple t : query.list($.geoloc.country,$.count.sum())) {
			list.add(new Object[] {t.get($.geoloc.country), t.get($.count.sum())});
		}
		
		list.add(0, new Object[] {"Country", "Downloads"});
		
		return list;
	}

	
	@Override
	public List<Object[]> downloadsGeography(LogType logType, String name) {
		List<Object[]> list = new ArrayList<Object[]>();		
		QLogEntity $ = QLogEntity.logEntity;
		
		JPQLQuery query = from($)
				.where(//IP is either null (records before 11/2014) or some real IP (not the special reserved one)
					$.addr.isNull().or($.addr.ne(LogUtils.UNIQUE_IP)))
				.groupBy($.geoloc.country,$.geoloc.region,$.geoloc.city)
				.orderBy($.geoloc.country.asc(),$.geoloc.region.asc(),$.geoloc.city.asc());
		
		//neither type nor name -> total counts by country
		if((logType == null || logType == LogType.TOTAL) && (name == null || name.isEmpty())) {
			query.where($.event.type.eq(LogType.TOTAL));
		} else if((logType == null || logType == LogType.TOTAL) && name != null && !name.isEmpty()) {
			//only name is provided -> counts by country for this name (ignore type)
			query.where($.event.name.eq(name));
		} else if(name == null || name.isEmpty()) {
			// type, except TOTAL, but no name -> counts by country for all events of the type
			query.where($.event.type.eq(logType));
		} else { // type, except TOTAL, and name -> counts by country for all events of the type, name
			query.where($.event.type.eq(logType).and($.event.name.eq(name)));
		}
		
		for(Tuple t : query.list($.geoloc.country,$.geoloc.region,$.geoloc.city,$.count.sum())) 
		{
			list.add(new Object[] {
						t.get($.geoloc.country), t.get($.geoloc.region), 
						t.get($.geoloc.city), t.get($.count.sum())
					});
		}
		
		list.add(0, new Object[] {"Country", "Region", "City", "Downloads"});
		
		return list;
	}

	@Override
	public List<Object[]> downloadsCountry(String countryCode, LogType logType,
			String name) {
		Assert.hasLength(countryCode); //not null/empty
		
		List<Object[]> list = new ArrayList<Object[]>();		
		QLogEntity $ = QLogEntity.logEntity;
		
		JPQLQuery query = from($) //from LogEntity table
				.where($.geoloc.country.eq(countryCode))
				.groupBy($.geoloc.city)
				.orderBy($.geoloc.city.asc());		
		//neither type nor name -> total counts by country
		if((logType == null || logType == LogType.TOTAL) && (name == null || name.isEmpty())) {
			query.where($.event.type.eq(LogType.TOTAL));
		} else if((logType == null || logType == LogType.TOTAL) && name != null && !name.isEmpty()) {
			//only name is provided -> counts by country for this name (ignore type)
			query.where($.event.name.eq(name));
		} else if(name == null || name.isEmpty()) {
			// type, except TOTAL, but no name -> counts by country for all events of the type
			query.where($.event.type.eq(logType));
		} else { // type, except TOTAL, and name -> counts by country for all events of the type, name
			query.where($.event.type.eq(logType).and($.event.name.eq(name)));
		}
		
		for(Tuple t : query.list($.geoloc.city,$.count.sum())) {
			list.add(new Object[] {t.get($.geoloc.city), t.get($.count.sum())});
		}
		
		list.add(0, new Object[] {"City", "Downloads"});
		
		return list;		
	}

	
	@Override
	public List<LogEvent> logEvents(LogType logType) {
		QLogEntity $ = QLogEntity.logEntity;
		if(logType != null)
			return from($).where($.event.type.eq(logType))
				.distinct().orderBy($.event.name.asc()).list($.event);
		else //list events of all types
			return from($).distinct()
				.orderBy($.event.type.asc(),$.event.name.asc()).list($.event);
	}


	@Override
	public Long downloads(String name) {
		Assert.hasLength(name);
		QLogEntity $ = QLogEntity.logEntity;
		return from($)
				.where($.event.name.eq(name), //and IP is either null (all records before 2014) or some real IP (not the special reserved one)
						$.addr.isNull().or($.addr.ne(LogUtils.UNIQUE_IP)))
				.uniqueResult($.count.sum());
	}


	@Override
	public Long uniqueIps(String name) {
		Assert.hasLength(name);
		QLogEntity $ = QLogEntity.logEntity;
		return from($)
			.where($.event.name.eq(name), //and IP is neither null not the reserved addr.
				$.addr.isNotNull().and($.addr.ne(LogUtils.UNIQUE_IP)))
			.uniqueResult($.addr.countDistinct());
	}
	

	@Override
	public List<String> listUniqueIps(LogType logType, String name) {
		Assert.notNull(logType);
		
		QLogEntity $ = QLogEntity.logEntity;
		
		if(name == null || name.isEmpty() ) {
			return from($)
				.distinct()
				.where($.event.type.eq(logType), //and except null and special addr.
					$.addr.isNotNull().and($.addr.ne(LogUtils.UNIQUE_IP)))
				.orderBy($.addr.asc())
				.list($.addr);
		} else { //ignore the type
			return from($)
				.distinct()
				.where($.event.name.eq(name), //and
					$.addr.isNotNull().and($.addr.ne(LogUtils.UNIQUE_IP)))
				.orderBy($.addr.asc())
				.list($.addr);
		}
	}


	@Override
	public Map<String, List<Object[]>> ipsTimeline(LogType logType, String name) {
		Map<String, List<Object[]>> timeline = new TreeMap<String, List<Object[]>>();			
		QLogEntity $ = QLogEntity.logEntity;
		if(name == null || name.isEmpty()) {
			Assert.notNull(logType);
			for(Tuple t : from($)
					.where($.event.type.eq(logType), //and the IP addr is not the special one or null
							$.addr.isNotNull().and($.addr.ne(LogUtils.UNIQUE_IP)))
					.groupBy($.event.name,$.date)
					.orderBy($.event.name.asc(),$.date.desc())
					.list($.event.name,$.date,$.addr.countDistinct())) 
			{
				String key = t.get($.event.name);
				List<Object[]> val = timeline.get(key);
				if(val == null) {
					val = new ArrayList<Object[]>();
					timeline.put(key, val);
				}
				val.add(new Object[] {t.get($.date), t.get($.addr.countDistinct())});
			}
		} 
		else { //name was provided; type does not matter anymore
			List<Object[]> val = new ArrayList<Object[]>();
			timeline.put(name, val);			
			for(Tuple t : from($)
					.where($.event.name.eq(name), //and
							$.addr.isNotNull().and($.addr.ne(LogUtils.UNIQUE_IP)))
					.groupBy($.date).orderBy($.date.desc())
					.list($.date,$.addr.countDistinct())) {
				val.add(new Object[] {t.get($.date), t.get($.addr.countDistinct())});
			}		
		}
		
		return timeline;
	}
	
	//Gets the cumulative no. unique IPs on each day per log type, name
	@Override
	public Map<String, List<Object[]>> ipsTimelineCum(LogType logType, String name) {
		Map<String, List<Object[]>> timeline = new TreeMap<String, List<Object[]>>();			
		
		QLogEntity $ = QLogEntity.logEntity;
		
		//get all days - from the first log entry to the last one
		final List<String> days = from($).orderBy($.date.asc()).distinct().list($.date);
		
		if(name == null || name.isEmpty()) {
			//if log event name is unspecified - 
			//do for all names in the category (logType)
			Assert.notNull(logType);
			
			//for each name in the logType category:
			for(String logName : from($)
					.where($.event.type.eq(logType))
					.distinct()
					.list($.event.name))
			{		
				//add the cum. no. IPs timeline for the logName
				addToIpsTimelineCumForName($, logType, logName, timeline, days);
			}
		} 
		else { 
			//given event name (a name belongs to one logType only),
			//get the cumulative no. unique IPs
			addToIpsTimelineCumForName($, logType, name, timeline, days);
		}
		
		return timeline;
	}
	
	private void addToIpsTimelineCumForName(
			QLogEntity $, 
			LogType logType, String logName,
			Map<String, List<Object[]>> timeline, 
			List<String> days) 
	{
		Assert.notNull(logType);
		Assert.notNull(logName);
		
		//first, try using already calculated counts (except for today and yesterday,
		//for which we are always to re-calculate...)
		Map<String, Long> cumUniqueIpsPerDay = cumUniqueIpsPerDayforName($, logName); //-this must be quite a fast query
		Assert.isTrue(!cumUniqueIpsPerDay.containsKey(LogUtils.yesterday()) && !cumUniqueIpsPerDay.containsKey(LogUtils.today()));				
		List<Object[]> val = new ArrayList<Object[]>();
		timeline.put(logName, val);
		for(String day : days) {
			//skip for days that the cumulative no. unique IPs was already calculated
			if(cumUniqueIpsPerDay.containsKey(day)) {
				val.add(new Object[] {day, cumUniqueIpsPerDay.get(day)});
				continue;
			}
			
			//calculate from scratch (the beginning of the log);
			//at some point this will happen only for yesterday and today days
			Long l = from($) //from LogEntity table
					.where($.event.name.eq(logName), //and
						$.addr.isNotNull().and($.addr.ne(LogUtils.UNIQUE_IP)), //and
						$.date.lt(day).or($.date.eq(day)))
					.uniqueResult($.addr.countDistinct());
			
			val.add(new Object[] {day, l});	
			
			//save or update the 'count' for the row: {day, name, LogUtils.UNIQUE_IP}
//			LogEntity logEntity = new LogEntity(day, new LogEvent(logType, logName), LogUtils.UNIQUE_IP);
//			logEntity.setCount(l);
//			((LogEntitiesRepository)this).save(logEntity);
			service.update(day, new LogEvent(logType, logName), LogUtils.UNIQUE_IP, l);
		}	
	}


	/*
	 * Gets the cumulative number of unique IP addresses, 
	 * given each log day (from the beginning up to now) and name, 
	 * using previously calculated 'counts' stored in special LogEntity
	 * objects (rows) under special unique key: {date, name, addr=LogUtils.UNIQUE_IP}.
	 * For dates where the key does not exist we have to re-calculate using a different method and store the value.
	 */
	private Map<String, Long> cumUniqueIpsPerDayforName(QLogEntity $, String name) {
		Map<String, Long> uniqueIpsByDay = new HashMap<String, Long>();

		// Get the counts from each row: day, name, LogUtils.UNIQUE_IP (if exists)
		// for log name, count no. unique IPs that occurred so far by each 'day' (inclusive);
		//
		// skip - for yesterday and today, because these counts might have already changed 
		// since last calculated (e.g., yesterday before the midnight and just now);
		// sorry for somewhat sub-optimal design, but I could not come up with a better solution, i.e., 
		// how to tell the final yesterday's IP counts from intermediate and to store that for each log name
		for(Tuple t : from($)
			.where($.event.name.eq(name), //and
					$.addr.eq(LogUtils.UNIQUE_IP), //and not yesterday
					$.date.ne(LogUtils.yesterday()), //and not today
					$.date.ne(LogUtils.today()))
			.list($.date,$.count))
		{	
			uniqueIpsByDay.put(t.get($.date), t.get($.count));  
		}

		return uniqueIpsByDay;
	}


	public Long totalRequests() {
		QLogEntity $ = QLogEntity.logEntity;
		return from($)
				.where($.event.type.eq(LogType.TOTAL), 
					//and IP is either null (records before 11/2014) or real IP (not the reserved one)
					$.addr.isNull().or($.addr.ne(LogUtils.UNIQUE_IP)))
				.uniqueResult($.count.sum());
	}


	public Long totalErrors() {
		QLogEntity $ = QLogEntity.logEntity;
		return from($)
				.where($.event.type.eq(LogType.ERROR), 
					//and IP is either null (records before 11/2014) or real IP (not the reserved one)
					$.addr.isNull().or($.addr.ne(LogUtils.UNIQUE_IP)))
				.uniqueResult($.count.sum());
	}


	public Long totalUniqueIps() {
		QLogEntity $ = QLogEntity.logEntity;
		return from($)
			.where($.event.type.eq(LogType.TOTAL), //and IP is not null nor the reserved one
				$.addr.isNotNull().and($.addr.ne(LogUtils.UNIQUE_IP)))
			.uniqueResult($.addr.countDistinct());
	}
		
}
