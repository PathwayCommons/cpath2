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

	private static final QLogEntity $ = QLogEntity.logEntity;
	
	public LogEntitiesRepositoryImpl() {
		super(LogEntity.class);
	}

	/* (non-Javadoc)
	 * @see cpath.log.service.LogService#downloadsTimeline()
	 */
	@Override
	public Map<String, List<Object[]>> downloadsTimeline() {
		Map<String, List<Object[]>> timeline = new TreeMap<String, List<Object[]>>();		
		
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
		
		List<Object[]> val = new ArrayList<Object[]>();
		
		for(Tuple t : from($).where($.event.type.eq(logType))
				.groupBy($.date).orderBy($.date.desc())
				.list($.date,$.count.sum())) {
			val.add(new Object[] {t.get($.date), t.get($.count.sum())});
		}

		return val;
	}
		
}
