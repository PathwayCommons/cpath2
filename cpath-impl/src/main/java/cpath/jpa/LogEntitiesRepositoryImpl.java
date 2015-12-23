package cpath.jpa;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.support.QueryDslRepositorySupport;
import org.springframework.util.Assert;

import com.mysema.query.Tuple;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.expr.BooleanExpression;
import com.mysema.query.types.path.StringPath;

import cpath.config.CPathSettings;
import cpath.dao.LogUtils;
import cpath.jpa.QLogEntity;

/**
 * @author rodche
 *
 */
class LogEntitiesRepositoryImpl extends QueryDslRepositorySupport 
	implements LogEntitiesRepositoryCustom {
	
	private final Logger LOG = LoggerFactory.getLogger(LogEntitiesRepositoryImpl.class);
	
	public LogEntitiesRepositoryImpl() {
		super(LogEntity.class);
	}
	
	private final CPathSettings instance = CPathSettings.getInstance();
		
	@Override
	public Map<String, List<Object[]>> downloadsTimeline(LogType logType, String name) 
	{
		Assert.notNull(logType);
		
		Map<String, List<Object[]>> timeline = new TreeMap<String, List<Object[]>>();			
		QLogEntity $ = QLogEntity.logEntity;		
		Predicate inRange = timelineRange($.date, instance.getLogStart(), instance.getLogEnd());
		
		//if log name is null - do for all events in the logType
		Predicate typeOrName = null; 
		if(name != null) { 
			typeOrName = (logType == LogType.FILE)
				? $.event.type.eq(logType).and($.event.name.containsIgnoreCase(name)) 
					: $.event.type.eq(logType).and($.event.name.equalsIgnoreCase(name));		
		} else {
			typeOrName = $.event.type.eq(logType);
			name = logType.description;
		}

		List<Object[]> val = new ArrayList<Object[]>();
		timeline.put(name, val);			
		for(Tuple t : from($)
				.where(typeOrName, inRange)
				.groupBy($.date).orderBy($.date.desc())
				.list($.date,$.count.sum())) {
			val.add(new Object[] {t.get($.date), t.get($.count.sum())});
		}		
		
		return timeline;
	}

	@Override 
	public Map<String, List<Object[]>> downloadsTimelineCum(LogType logType, String name) 
	{
		Assert.notNull(logType);
		
		Map<String, List<Object[]>> timeline = downloadsTimeline(logType, name);					
		//build the cumulative timeline(s) from the daily counts,
		//which are additive.
		for(String key : timeline.keySet()) {
			List<Object[]> points = timeline.get(key);
			int len = points.size(); //the no. days in the timeline
			if(len>1) {
				//we want to sum up starting from the first day (oldest); so -
				//will iterate in the reverse order (as points were sorted by date in descending order)
				//- from the last to the second point:
				for(int i=len-1; i>0; i--) {
					Object[] earlierDay = points.get(i);
					Object[] laterDay = points.get(i-1);
					laterDay[1] = (Long)(laterDay[1]) + (Long)(earlierDay[1]);
				}
			}
		}
		// return converted cumulative timeline		
		return timeline;
	}	
	
	@Override
	public List<Object[]> downloadsWorld(LogType logType, String name) {
		Assert.notNull(logType);
		
		List<Object[]> list = new ArrayList<Object[]>();		
		QLogEntity $ = QLogEntity.logEntity;
		
		JPQLQuery query = from($)
				.groupBy($.geoloc.country)
				.orderBy($.geoloc.country.asc());
		
		//neither type nor name -> total counts by country
		if(logType == LogType.TOTAL && (name == null || name.isEmpty())) {
			query.where($.event.type.eq(LogType.TOTAL));
		} else if(logType == LogType.TOTAL && name != null && !name.isEmpty()) {
			//only name is provided -> counts by country for this name (ignore this special type)
			query.where($.event.name.equalsIgnoreCase(name));
		} else if(name == null || name.isEmpty()) {
			// type, except TOTAL, but no name -> counts by country for all events of the type
			query.where($.event.type.eq(logType));
		} else { // type, except TOTAL, and name -> counts by country for all events of the type, name
			query.where($.event.type.eq(logType)); //not finished yet...
			//a special case for type:FILE to support partial filenames (e.g., 'reactome.biopax')
			if(logType == LogType.FILE) {
				query.where($.event.name.containsIgnoreCase(name));
			} else {
				query.where($.event.name.equalsIgnoreCase(name));
			}
		}
		
		for(Tuple t : query.list($.geoloc.country,$.count.sum())) {
			list.add(new Object[] {t.get($.geoloc.country), t.get($.count.sum())});
		}
		
		list.add(0, new Object[] {"Country", "Downloads"});
		
		return list;
	}

	
	@Override
	public List<Object[]> downloadsGeography(LogType logType, String name) {
		Assert.notNull(logType);
		
		List<Object[]> list = new ArrayList<Object[]>();		
		QLogEntity $ = QLogEntity.logEntity;
		
		JPQLQuery query = from($)
				.groupBy($.geoloc.country,$.geoloc.region,$.geoloc.city)
				.orderBy($.geoloc.country.asc(),$.geoloc.region.asc(),$.geoloc.city.asc());
		
		//neither type nor name -> total counts by country
		if(logType == LogType.TOTAL && (name == null || name.isEmpty())) {
			query.where($.event.type.eq(LogType.TOTAL));
		} else if(logType == LogType.TOTAL && name != null && !name.isEmpty()) {
			//only name is provided -> counts by country for this name (ignore special type)
			query.where($.event.name.equalsIgnoreCase(name));
		} else if(name == null || name.isEmpty()) {
			// type, except TOTAL, but no name -> counts by country for all events of the type
			query.where($.event.type.eq(logType));
		} else { // type, except TOTAL, and name -> counts by country for all events of the type, name
			query.where($.event.type.eq(logType)); //not finished yet...
			//a special case for type:FILE to support partial filenames (e.g., 'reactome.biopax')
			if(logType == LogType.FILE) {
				query.where($.event.name.containsIgnoreCase(name));
			} else {
				query.where($.event.name.equalsIgnoreCase(name));
			}
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
	public List<Object[]> downloadsCountry(String countryCode, LogType logType, String name) 
	{
		Assert.notNull(logType);
		Assert.hasLength(countryCode); //not null/empty
		
		List<Object[]> list = new ArrayList<Object[]>();		
		QLogEntity $ = QLogEntity.logEntity;
		
		JPQLQuery query = from($) //from LogEntity table
				.where($.geoloc.country.eq(countryCode)) //TODO add dates range restriction?
				.groupBy($.geoloc.city)
				.orderBy($.geoloc.city.asc());		
		//neither type nor name -> total counts by country
		if(logType == LogType.TOTAL && (name == null || name.isEmpty())) {
			query.where($.event.type.eq(LogType.TOTAL));
		} else if(logType == LogType.TOTAL && name != null && !name.isEmpty()) {
			//only name is provided -> counts by country for this name (ignore type)
			query.where($.event.name.equalsIgnoreCase(name));
		} else if(name == null || name.isEmpty()) {
			// type, except TOTAL, but no name -> counts by country for all events of the type
			query.where($.event.type.eq(logType));
		} else { // type, except TOTAL, and name -> counts by country for all events of the type, name
			query.where($.event.type.eq(logType)); //not finished yet...
			//a special case for type:FILE to support partial filenames (e.g., 'reactome.biopax')
			if(logType == LogType.FILE) {
				query.where($.event.name.containsIgnoreCase(name));
			} else {
				query.where($.event.name.equalsIgnoreCase(name));
			}
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
				.where($.event.name.equalsIgnoreCase(name))
				.uniqueResult($.count.sum());
	}


	@Override
	public Long uniqueIps(String name) {
		Assert.hasLength(name);
		QLogEntity $ = QLogEntity.logEntity;
		return from($)
			.where($.event.name.equalsIgnoreCase(name), $.addr.isNotNull())
			.uniqueResult($.addr.countDistinct());
	}
	

	@Override
	public List<String> listUniqueIps(LogType logType, String name) {
		Assert.notNull(logType);
		
		QLogEntity $ = QLogEntity.logEntity;
		
		if(name == null || name.isEmpty() ) {
			return from($)
				.distinct()
				.where($.event.type.eq(logType), $.addr.isNotNull())
				.orderBy($.addr.asc())
				.list($.addr);
		} else { 
			JPQLQuery query = from($).distinct()
				.where($.event.type.eq(logType), $.addr.isNotNull())
				.orderBy($.addr.asc());	//query building is not finished yet...		
			if(logType == LogType.FILE) {
				//a special case for type:FILE to support partial names (e.g., 'reactome.biopax')
				query.where($.event.name.containsIgnoreCase(name));
			} else {
				query.where($.event.name.equalsIgnoreCase(name));
			}
			return query.list($.addr);
		}
	}


	@Override
	public Map<String, List<Object[]>> ipsTimeline(LogType logType, String name) {
		Assert.notNull(logType);
		
		Map<String, List<Object[]>> timeline = new TreeMap<String, List<Object[]>>();			
		QLogEntity $ = QLogEntity.logEntity;
		Predicate inRange = timelineRange($.date, instance.getLogStart(), instance.getLogEnd());
		
		//if log name is null - do for all events in the logType
		Predicate typeOrName = null; 
		if(name != null) { 
			typeOrName = (logType == LogType.FILE)
					? $.event.type.eq(logType).and($.event.name.containsIgnoreCase(name)) 
						: $.event.type.eq(logType).and($.event.name.equalsIgnoreCase(name));
		} else {
			typeOrName = $.event.type.eq(logType);
			name = logType.description;
		}

		List<Object[]> val = new ArrayList<Object[]>();
		timeline.put(name, val);			
		for(Tuple t : from($)
				.where(typeOrName, $.addr.isNotNull(), inRange) //',' means 'AND'
				.groupBy($.date).orderBy($.date.desc())
				.list($.date,$.addr.countDistinct())) {
			val.add(new Object[] {t.get($.date), t.get($.addr.countDistinct())});
		}		
		
		return timeline;
	}
	
	//Gets the cumulative no. unique IPs on each day per log type, name
	@Override
	public Map<String, List<Object[]>> ipsTimelineCum(LogType logType, String logName) {
		Assert.notNull(logType);
		
		Map<String, List<Object[]>> timeline = new TreeMap<String, List<Object[]>>();			
		QLogEntity $ = QLogEntity.logEntity;

		//if logName is null - will do for all events in the logType
		Predicate typeOrName = null; 
		if(logName != null) { 
			typeOrName = (logType == LogType.FILE)
					? $.event.type.eq(logType).and($.event.name.containsIgnoreCase(logName)) 
						: $.event.type.eq(logType).and($.event.name.equalsIgnoreCase(logName));
		} else {
			typeOrName = $.event.type.eq(logType);
			logName = logType.description;
		}
		
		Predicate inRange = timelineRange($.date, instance.getLogStart(), instance.getLogEnd());
		Predicate sinceStart = (instance.getLogStart() != null) ? $.date.goe(instance.getLogStart()) : null;
		
		//get all logged days from given log start to the end 
		//(some dates may be missing - if server was down or no such requests happened)
		final List<String> days = from($)
				.where(typeOrName, inRange)
				.orderBy($.date.asc()).distinct()
				.list($.date);
		
		List<Object[]> val = new ArrayList<Object[]>();
		timeline.put(logName, val);
		for(String day : days) {
			//calculate from the beginning of the log;
			Long l = from($) //from LogEntity table
				.where(typeOrName, //, and IP is not null
					$.addr.isNotNull(), //, and - on that day and earlier days, since given start date
					$.date.loe(day), sinceStart) //TODO remove the sinceStart? (always account for all days in the past?)
				.uniqueResult($.addr.countDistinct());
			
			if(l==null) 
				l = 0L;
			
			val.add(new Object[] {day, l});	
		}	
	
		return timeline;
	}

	public Long totalRequests() {
		QLogEntity $ = QLogEntity.logEntity;
		return from($)
				.where($.event.type.eq(LogType.TOTAL))
				.uniqueResult($.count.sum());
	}

	public Long totalErrors() {
		QLogEntity $ = QLogEntity.logEntity;
		return from($)
				.where($.event.type.eq(LogType.ERROR))
				.uniqueResult($.count.sum());
	}

	public Long totalUniqueIps() {
		QLogEntity $ = QLogEntity.logEntity;
		return from($)
			.where($.event.type.eq(LogType.TOTAL), $.addr.isNotNull())
			.uniqueResult($.addr.countDistinct());
	}
	
	
	//logStart, logEnd are valid ISO formatted date strings or null (- unrestricted)
	private Predicate timelineRange(StringPath date, String logStart, String logEnd) 
	{	
		//just a quick dirty test (caller methods are to make sure parameters here are valid)
		Assert.isTrue(logStart==null || logStart.length()==10, "bad logStart value:" + logStart);
		Assert.isTrue(logEnd==null || logEnd.length()==10, "bad logEnd value:" + logEnd);
		
		BooleanExpression goe = null;//greater or equal
		if(logStart != null) {
			goe = date.goe(logStart);
		} else {
			//default from a year back from now
			Calendar cal = Calendar.getInstance(); //defs to current time
			cal.add(Calendar.YEAR, -1);
			String yearAgo = LogUtils.ISO_DATE_FORMAT.format(cal.getTime()); 
			goe = date.goe(yearAgo);
		}
		
		BooleanExpression loe = (logEnd != null && 
			LogUtils.today().compareTo(instance.getLogEnd()) > 0)
				? date.loe(logEnd) : null;
				
		Predicate range;
		
		if(goe!=null && loe!=null)
			range = goe.and(loe);
		else if(goe!=null)
			range = goe;
		else if(loe!=null){
			range = loe;
		} else {
			range = null; //null is simply ignored in a querydsl q.where(...)
		}
		
		return range;
	}
}
