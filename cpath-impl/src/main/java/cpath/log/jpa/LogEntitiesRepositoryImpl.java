/**
 * 
 */
package cpath.log.jpa;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.data.jpa.repository.support.QueryDslRepositorySupport;
import org.springframework.util.Assert;

import com.mysema.query.Tuple;

import cpath.log.LogType;

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
	 * @see cpath.log.service.LogService#downloadsTimeline()
	 */
	@Override
	public Map<String, List<Object[]>> downloadsTimeline() {
		Map<String, List<Object[]>> timeline = new TreeMap<String, List<Object[]>>();		
		
		QLogEntity $ = QLogEntity.logEntity;
		
		for(Tuple t : from($).groupBy($.event.type,$.date).orderBy($.event.type.asc(),$.date.desc())
				.list($.event.type,$.date,$.count.sum())) 
		{
			String key = t.get($.event.type).description;
			List<Object[]> val = timeline.get(key);
			if(val == null) {
				val = new ArrayList<Object[]>();
				timeline.put(key, val);
			}
			val.add(new Object[] {t.get($.date), t.get($.count.sum())});
		}
		
		return timeline;
	}

	/* (non-Javadoc)
	 * @see cpath.log.service.LogService#downloadsTimeline(cpath.log.LogType, java.lang.String)
	 */
	@Override
	public Map<String, List<Object[]>> downloadsTimeline(LogType logType,
			String name) {
		Assert.notNull(logType);
		
		Map<String, List<Object[]>> timeline = new TreeMap<String, List<Object[]>>();	
		
		QLogEntity $ = QLogEntity.logEntity;
		
		if(name == null || name.isEmpty()) {
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
		else { //name was provided
			List<Object[]> val = new ArrayList<Object[]>();
			timeline.put(name, val);
			
			for(Tuple t : from($)
					.where($.event.type.eq(logType).and($.event.name.eq(name)))
					.groupBy($.date).orderBy($.date.desc())
					.list($.date,$.count.sum())) {
				val.add(new Object[] {t.get($.date), t.get($.count.sum())});
			}		
		}
		
		//add total
		timeline.put(LogType.TOTAL.description, downloadsTimeline(LogType.TOTAL));
		
		return timeline;
	}


	// total counts by date for one type only
	private List<Object[]> downloadsTimeline(LogType logType) {
		Assert.notNull(logType);
		
		List<Object[]> list = new ArrayList<Object[]>();		
		QLogEntity $ = QLogEntity.logEntity;
		
		for(Tuple t : from($).where($.event.type.eq(logType))
				.groupBy($.date).orderBy($.date.desc())
				.list($.date,$.count.sum())) {
			list.add(new Object[] {t.get($.date), t.get($.count.sum())});
		}

		return list;
	}

	
	@Override
	public List<Object[]> downloadsCountry(LogType logType, String name) {
		
		List<Object[]> list = new ArrayList<Object[]>();		
		QLogEntity $ = QLogEntity.logEntity;
		
		//neither type nor name -> total counts by country
		if((logType == null || logType == LogType.TOTAL) && (name == null || name.isEmpty())) {
			for(Tuple t : from($)
					.where($.event.type.eq(LogType.TOTAL))
					.groupBy($.geoloc.country).orderBy($.geoloc.country.asc())
					.list($.geoloc.country,$.count.sum())) {
				list.add(new Object[] {t.get($.geoloc.country), t.get($.count.sum())});
			}
		} else if((logType == null || logType == LogType.TOTAL) && name != null && name.isEmpty()) {
			//only name is provided -> counts by country for this name (ignore type)
			for(Tuple t : from($)
					.where($.event.name.eq(name))
					.groupBy($.geoloc.country).orderBy($.geoloc.country.asc())
					.list($.geoloc.country,$.count.sum())) {
				list.add(new Object[] {t.get($.geoloc.country), t.get($.count.sum())});
			}
		} else if(name == null || name.isEmpty()) {
			// type, except TOTAL, but no name -> counts by country for all events of the type
			for(Tuple t : from($)
					.where($.event.type.eq(logType))
					.groupBy($.geoloc.country).orderBy($.geoloc.country.asc())
					.list($.geoloc.country,$.count.sum())) {
				list.add(new Object[] {t.get($.geoloc.country), t.get($.count.sum())});
			}
		} else { // type, except TOTAL, and name -> counts by country for all events of the type, name
			for(Tuple t : from($)
					.where($.event.type.eq(logType).and($.event.name.eq(name)))
					.groupBy($.geoloc.country).orderBy($.geoloc.country.asc())
					.list($.geoloc.country,$.count.sum())) {
				list.add(new Object[] {t.get($.geoloc.country), t.get($.count.sum())});
			}
		}
		
		return list;
	}

	
	//TODO add GeoIP City db to LogUtils and lookup 'region' in a future version
	@Override
	public List<Object[]> downloadsGeography(LogType logType, String name) {
		throw new UnsupportedOperationException("Not implemented yet (requires GeoIp City database, etc.)");
	}
		
}
