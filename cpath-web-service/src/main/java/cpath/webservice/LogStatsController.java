package cpath.webservice;

import java.util.*;

import cpath.jpa.LogEvent;
import cpath.jpa.LogType;
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
			Class.forName("cpath.dao.LogUtils");
		} catch (ClassNotFoundException e) {
			log.error("Class.forName(cpath.dao.LogUtils) failed");
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
    	for(LogEvent e : service.log().logEvents(logType)) {
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
    	for(LogEvent e : service.log().logEvents(logType)) {
    		if(e.getName().equals(name))
    			ret.add(0, new String[]{e.getType().toString(), e.getName()});
    		else 
    			ret.add(new String[]{e.getType().toString(), e.getName()});
    	}
    	return ret;
    } 
       
    //timelines (no. queries by type, date)
    
	@RequestMapping("/{logType}/{name}/timeline")
    public @ResponseBody Map<String,List<Object[]>> timeline(@PathVariable LogType logType, @PathVariable String name) {		
    	return service.log().downloadsTimeline(logType, name);
    }    
    
    @RequestMapping("/{logType}/timeline")
    public @ResponseBody Map<String,List<Object[]>> timeline(@PathVariable LogType logType) {		
    	return service.log().downloadsTimeline(logType, null);
    }
	
	@RequestMapping("/timeline")
    public @ResponseBody Map<String,List<Object[]>> timeline() {		
    	return service.log().downloadsTimeline(LogType.TOTAL, null);
    }
	
	//iptimelines (no. unique client IP addresses by request type on each day)
	
	@RequestMapping("/iptimeline")
    public @ResponseBody Map<String,List<Object[]>> iptimeline() {		
    	return service.log().ipsTimeline(LogType.TOTAL, null);
    }
	
	@RequestMapping("/{logType}/{name}/iptimeline")
    public @ResponseBody Map<String,List<Object[]>> iptimeline(@PathVariable LogType logType, @PathVariable String name) {		
    	return service.log().ipsTimeline(logType, name);
    }    
    
    @RequestMapping("/{logType}/iptimeline")
    public @ResponseBody Map<String,List<Object[]>> iptimeline(@PathVariable LogType logType) {		
    	return service.log().ipsTimeline(logType, null);
    }
    
    //cumulative iptimeline, -  list of [date, total no. unique IPs from the beginning of time/service to this date]
	@RequestMapping("/iptimelinecum")
    public @ResponseBody Map<String,List<Object[]>> iptimelinecum() {		
    	return service.log().ipsTimelineCum(LogType.TOTAL, null);
    }
	
	@RequestMapping("/{logType}/{name}/iptimelinecum")
    public @ResponseBody Map<String,List<Object[]>> iptimelinecum(@PathVariable LogType logType, @PathVariable String name) {		
    	return service.log().ipsTimelineCum(logType, name);
    }    
    
    @RequestMapping("/{logType}/iptimelinecum")
    public @ResponseBody Map<String,List<Object[]>> iptimelinecum(@PathVariable LogType logType) {		
    	return service.log().ipsTimelineCum(logType, null);
    }   
    

    //list of unique client IP addresses by request type
    
	@RequestMapping("/iplist")
    public @ResponseBody List<String> ips() {		
    	return service.log().listUniqueIps(LogType.TOTAL, null);
    }
	
	@RequestMapping("/{logType}/{name}/iplist")
    public @ResponseBody List<String> ips(@PathVariable LogType logType, @PathVariable String name) {
		return service.log().listUniqueIps(logType, name);
    }    
    
    @RequestMapping("/{logType}/iplist")
    public @ResponseBody List<String> ips(@PathVariable LogType logType) {		
    	return service.log().listUniqueIps(logType, null);
    }	
	
	//geo (no. queries by type, geolocation)
	
	@RequestMapping("/geography/world")
    public @ResponseBody List<Object[]> geographyWorld() {		
    	return service.log().downloadsWorld(null, null);
    }
    
	@RequestMapping("/{logType}/geography/world")
    public @ResponseBody List<Object[]> geographyWorld(@PathVariable LogType logType) {		
    	return service.log().downloadsWorld(logType, null);
    }
    
	@RequestMapping("/{logType}/{name}/geography/world")
    public @ResponseBody List<Object[]> geographyWorld(@PathVariable LogType logType, @PathVariable String name) {		
    	return service.log().downloadsWorld(logType, name);
    }
    
    
	@RequestMapping("/geography/all")
    public @ResponseBody List<Object[]> geographyAll() {		
    	return service.log().downloadsGeography(null, null);
    }
    
	@RequestMapping("/{logType}/geography/all")
    public @ResponseBody List<Object[]> geographyAll(@PathVariable LogType logType) {		
    	return service.log().downloadsGeography(logType, null);
    }
    
	@RequestMapping("/{logType}/{name}/geography/all")
    public @ResponseBody List<Object[]> geographyAll(@PathVariable LogType logType, 
    		@PathVariable String name) {		
    	return service.log().downloadsGeography(logType, name);
    }    
    
 
	@RequestMapping("/geography/country/{code}")
    public @ResponseBody List<Object[]> geographyCountry(@PathVariable String code) {		
    	return service.log().downloadsCountry(code, null, null);
    }
    
	@RequestMapping("/{logType}/geography/country/{code}")
    public @ResponseBody List<Object[]> geographyCountry(@PathVariable LogType logType,
    		@PathVariable String code) {		
    	return service.log().downloadsCountry(code, logType, null);
    }
    
	@RequestMapping("/{logType}/{name}/geography/country/{code}")
    public @ResponseBody List<Object[]> geographyCountry(@PathVariable LogType logType, 
    		@PathVariable String name, @PathVariable String code) {		
    	return service.log().downloadsCountry(code, logType, name);
    }
}