package cpath.webservice;

import java.util.*;

import cpath.log.LogType;
import cpath.log.jpa.LogEvent;
import cpath.webservice.args.binding.LogTypeEditor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;


/**
 * RESTful web controller for the access log pages.
 * 
 * @author rodche
 */
@Controller
@RequestMapping("/log")
public class LogStatsController extends BasicController {
	private static final Logger log = LoggerFactory.getLogger(LogStatsController.class);   
	
	
	public LogStatsController() {
    	try {
			Class.forName("cpath.log.LogUtils");
		} catch (ClassNotFoundException e) {
			log.error("Class.forName(cpath.log.LogUtils) failed");
		}
	}
	
	@InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(LogType.class, new LogTypeEditor());
	}
    
    // XML/JSON web services (for the stats.jsp view using stats.js, given current context path)
 
    //if arg is not null, then it's the first element in the returned list
    private List<String[]> categories(LogType logType) {
    	List<String[]> ret = new ArrayList<String[]>();
    	for(LogType t : LogType.values()) {
    		String s[] = new String[2];
    		s[0] = t.toString();
    		s[1] = "";
    		if(t==logType) ret.add(0, s); else ret.add(s);
    	}   	
    	return ret;
    }     
    
	@RequestMapping("/events")
    public @ResponseBody List<String[]> logEvents() {
    	List<String[]> ret = categories(null);
    	return ret;
    } 

    @RequestMapping("/{logType}/events")
    public @ResponseBody List<String[]> logEvents(@PathVariable LogType logType) {	
    	//add all the events in the same category (type); 
    	//but make given type the first element in the list (selected)
    	List<String[]> ret = categories(logType);
    	for(LogEvent e : logEntitiesRepository.logEvents(logType)) {
    		ret.add(new String[]{e.getType().toString(), e.getName()});
    	}
    	return ret;
    }    
    
    @RequestMapping("/{logType}/{name}/events")
    public @ResponseBody List<String[]> logEvents(@PathVariable LogType logType, 
    		@PathVariable String name) {
    	List<String[]> ret = categories(logType);
    	//add all the events in the same category (type); 
    	// but make the one with given type,name the first element in the list
    	for(LogEvent e : logEntitiesRepository.logEvents(logType)) {
    		if(e.getName().equals(name))
    			ret.add(0, new String[]{e.getType().toString(), e.getName()});
    		else 
    			ret.add(new String[]{e.getType().toString(), e.getName()});
    	}
    	return ret;
    } 
       
    //timeline
    
	@RequestMapping("/{logType}/{name}/timeline")
    public @ResponseBody Map<String,List<Object[]>> timeline(@PathVariable LogType logType, @PathVariable String name) {		
    	return logEntitiesRepository.downloadsTimeline(logType, name);
    }    
    
    @RequestMapping("/{logType}/timeline")
    public @ResponseBody Map<String,List<Object[]>> timeline(@PathVariable LogType logType) {		
    	return logEntitiesRepository.downloadsTimeline(logType, null);
    }
	
	@RequestMapping("/timeline")
    public @ResponseBody Map<String,List<Object[]>> timeline() {		
    	return logEntitiesRepository.downloadsTimeline(LogType.TOTAL, null);
    }
	
	//geo
	
	@RequestMapping("/geography/world")
    public @ResponseBody List<Object[]> geographyWorld() {		
    	return logEntitiesRepository.downloadsWorld(null, null);
    }
    
	@RequestMapping("/{logType}/geography/world")
    public @ResponseBody List<Object[]> geographyWorld(@PathVariable LogType logType) {		
    	return logEntitiesRepository.downloadsWorld(logType, null);
    }
    
	@RequestMapping("/{logType}/{name}/geography/world")
    public @ResponseBody List<Object[]> geographyWorld(@PathVariable LogType logType, @PathVariable String name) {		
    	return logEntitiesRepository.downloadsWorld(logType, name);
    }
    
    
	@RequestMapping("/geography/all")
    public @ResponseBody List<Object[]> geographyAll() {		
    	return logEntitiesRepository.downloadsGeography(null, null);
    }
    
	@RequestMapping("/{logType}/geography/all")
    public @ResponseBody List<Object[]> geographyAll(@PathVariable LogType logType) {		
    	return logEntitiesRepository.downloadsGeography(logType, null);
    }
    
	@RequestMapping("/{logType}/{name}/geography/all")
    public @ResponseBody List<Object[]> geographyAll(@PathVariable LogType logType, 
    		@PathVariable String name) {		
    	return logEntitiesRepository.downloadsGeography(logType, name);
    }    
    
 
	@RequestMapping("/geography/country/{code}")
    public @ResponseBody List<Object[]> geographyCountry(@PathVariable String code) {		
    	return logEntitiesRepository.downloadsCountry(code, null, null);
    }
    
	@RequestMapping("/{logType}/geography/country/{code}")
    public @ResponseBody List<Object[]> geographyCountry(@PathVariable LogType logType,
    		@PathVariable String code) {		
    	return logEntitiesRepository.downloadsCountry(code, logType, null);
    }
    
	@RequestMapping("/{logType}/{name}/geography/country/{code}")
    public @ResponseBody List<Object[]> geographyCountry(@PathVariable LogType logType, 
    		@PathVariable String name, @PathVariable String code) {		
    	return logEntitiesRepository.downloadsCountry(code, logType, name);
    }
}