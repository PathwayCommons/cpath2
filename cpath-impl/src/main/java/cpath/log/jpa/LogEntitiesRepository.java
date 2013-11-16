package cpath.log.jpa;

import org.springframework.data.querydsl.QueryDslPredicateExecutor;

import cpath.log.LogType;


/**
 * Main repository interface to be used by spring-data 
 * (methods follow spring-data naming and signature conventions).
 * 
 * @author rodche
 *
 */
public interface LogEntitiesRepository extends BaseRepository<LogEntity, Long>, 
	QueryDslPredicateExecutor<LogEntity>, LogEntitiesRepositoryCustom {
	
	LogEntity findByTypeAndNameAndGeolocCountryAndDate(LogType type, String name, String countryCode, String date);
		
}
