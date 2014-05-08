/**
 * 
 */
package cpath.jpa;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.data.jpa.repository.support.QueryDslRepositorySupport;
import org.springframework.util.Assert;

import com.mysema.query.Tuple;
import com.mysema.query.jpa.JPQLQuery;

import cpath.jpa.QLogEntity;

/**
 * @author rodche
 *
 */
class LogEntitiesRepositoryImpl extends QueryDslRepositorySupport 
	implements LogEntitiesRepositoryCustom {
	
	public LogEntitiesRepositoryImpl() {
		super(LogEntity.class);
	}


	/* (non-Javadoc)
	 * @see cpath.log.service.LogService#downloadsTimeline(cpath.log.LogType, java.lang.String)
	 */
	@Override
	public Map<String, List<Object[]>> downloadsTimeline(LogType logType,
			String name) {
		Map<String, List<Object[]>> timeline = new TreeMap<String, List<Object[]>>();			
		QLogEntity $ = QLogEntity.logEntity;
		if(name == null || name.isEmpty()) {
			Assert.notNull(logType);
			for(Tuple t : from($).where($.event.type.eq(logType))
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
					.where($.event.name.eq(name))
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
		
		JPQLQuery query = from($).groupBy($.geoloc.country).orderBy($.geoloc.country.asc());
		
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
		
		for(Tuple t : query.list($.geoloc.country,$.geoloc.region,
				$.geoloc.city,$.count.sum())) {
			list.add(new Object[] {t.get($.geoloc.country), t.get($.geoloc.region), 
					t.get($.geoloc.city), t.get($.count.sum())});
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
		
		JPQLQuery query = from($).where($.geoloc.country.eq(countryCode))
				.groupBy($.geoloc.city).orderBy($.geoloc.city.asc());		
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
		else 
			return from($).distinct()
				.orderBy($.event.type.asc(),$.event.name.asc()).list($.event);
	}


	@Override
	public Long downloads(String name) {
		Assert.hasLength(name);
		QLogEntity $ = QLogEntity.logEntity;
		return from($).where($.event.name.eq(name)).uniqueResult($.count.sum());
	}
		
}
