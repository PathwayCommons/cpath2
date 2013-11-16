/**
 * 
 */
package cpath.log.jpa;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.data.jpa.repository.support.QueryDslRepositorySupport;

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
		
		for(Tuple t : from($).groupBy($.type,$.date).orderBy($.type.asc(),$.date.desc())
				.list($.type,$.date,$.count.sum())) 
		{
			String key = t.get($.type).description;
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void log(String date, LogType logType, String name, String ipAddress) {
		// TODO Auto-generated method stub
		
	}

}
