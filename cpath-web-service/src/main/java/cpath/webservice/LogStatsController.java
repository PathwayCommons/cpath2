package cpath.webservice;

import java.util.*;


import cpath.config.CPathSettings;
import cpath.log.LogType;
import cpath.log.jpa.LogEvent;
import cpath.webservice.args.binding.LogTypeEditor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;


/**
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
    
    /**
     * Makes current cpath2 instance properties 
     * available to all (JSP) views.
     * @return
     */
    @ModelAttribute("cpath")
    public CPathSettings instance() {
    	return CPathSettings.getInstance();
    }
    
    // JSP Views
    
    // is /log
    @RequestMapping(method = RequestMethod.GET) 
    public String log() {
    	return "redirect:/log/stats";
    }
    
    // is /log/
    @RequestMapping(value="/")
    public String logSlash() {
    	return "redirect:/log/stats";
    }
    
    
    @RequestMapping("/stats")
    public String allStats(Model model) {
    	model.addAttribute("summary_for", "All Categories (Total)");
    	return "stats";
    }	
        
    @RequestMapping("/{logType}/stats")
    public String statsByType(Model model, @PathVariable LogType logType) {
    	model.addAttribute("summary_for", "Category: " + logType);
    	return "stats";
    }
    
    @RequestMapping("/{logType}/{name}/stats")
    public String statsByType(Model model, @PathVariable LogType logType, 
    		@PathVariable String name) {
    	model.addAttribute("summary_for", "Category: " + logType + ", name: " + name);
    	return "stats";
    }

    
    // XML/JSON web services (for the stats.jsp view using stats.js, given current context path)
 
    @RequestMapping("/types")
    public @ResponseBody String[] logTypes() {
    	LogType[] t  = LogType.values();
    	String s[] = new String[t.length];
    	for(int i = 0; i < t.length; i++) {
    		s[i] = (t[i]).toString();
    	}
    	return s;
    }     
    
	@RequestMapping("/{logType}/{name}/events")
    public @ResponseBody List<LogEvent> logEvents(@PathVariable LogType logType, @PathVariable String name) {		
    	return logEntitiesRepository.logEvents(logType); //same as without any name
    } 
    
    @RequestMapping("/{logType}/events")
    public @ResponseBody List<LogEvent> logEvents(@PathVariable LogType logType) {		
    	return logEntitiesRepository.logEvents(logType);
    } 
	
	@RequestMapping("/events")
    public @ResponseBody List<LogEvent> logEvents() {		
    	return logEntitiesRepository.logEvents(null);
    }     
    
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