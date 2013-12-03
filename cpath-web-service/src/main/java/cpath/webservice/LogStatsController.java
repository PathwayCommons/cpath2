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
    
    @ModelAttribute("log_types")
    public String[] logTypes() {
    	LogType[] t  = LogType.values();
    	String s[] = new String[t.length];
    	for(int i = 0; i < t.length; i++) {
    		s[i] = (t[i]).toString();
    	}
    	return s;
    }
        
    
    // JSP Views
    
    @RequestMapping("/stats")
    public String allStats(Model model) {
    	model.addAttribute("summary_for", "Everything");
    	return "stats";
    }	
        
    @RequestMapping("/{logType}/stats")
    public String statsByType(Model model, @PathVariable LogType logType) {
    	model.addAttribute("summary_for", "Category: " + logType);
    	model.addAttribute("log_type", logType);
    	return "stats";
    }
    
    @RequestMapping("/{logType}/{name}/stats")
    public String statsByType(Model model, @PathVariable LogType logType, 
    		@PathVariable String name) {
    	model.addAttribute("summary_for", "Category: " + logType + ", name: " + name);
    	model.addAttribute("log_type", logType);
    	model.addAttribute("log_name", name);
    	return "stats";
    }

    
    // XML/JSON web services
    
	@RequestMapping(value = "/{logType}/events")
    public @ResponseBody List<LogEvent> logEvents(@PathVariable LogType logType) {		
    	return logEntitiesRepository.logEvents(logType);
    } 
	
	@RequestMapping(value = "/events")
    public @ResponseBody List<LogEvent> logEvents() {		
    	return logEntitiesRepository.logEvents(null);
    }     
    
	@RequestMapping(value = "/{logType}/{name}/timeline")
    public @ResponseBody Map<String,List<Object[]>> timeline(@PathVariable LogType logType, @PathVariable String name) {		
    	return logEntitiesRepository.downloadsTimeline(logType, name);
    }    
    
    @RequestMapping(value = "/{logType}/timeline")
    public @ResponseBody Map<String,List<Object[]>> timeline(@PathVariable LogType logType) {		
    	return logEntitiesRepository.downloadsTimeline(logType, null);
    }
	
	@RequestMapping(value = "/timeline")
    public @ResponseBody Map<String,List<Object[]>> timeline() {		
    	return logEntitiesRepository.downloadsTimeline(LogType.TOTAL, null);
    }
	
	@RequestMapping(value = "/geography/world")
    public @ResponseBody List<Object[]> geographyWorld() {		
    	return logEntitiesRepository.downloadsWorld(null, null);
    }
    
	@RequestMapping(value = "/{logType}/geography/world")
    public @ResponseBody List<Object[]> geographyWorld(@PathVariable LogType logType) {		
    	return logEntitiesRepository.downloadsWorld(logType, null);
    }
    
	@RequestMapping(value = "/{logType}/{name}/geography/world")
    public @ResponseBody List<Object[]> geographyWorld(@PathVariable LogType logType, @PathVariable String name) {		
    	return logEntitiesRepository.downloadsWorld(logType, name);
    }
    
    
	@RequestMapping(value = "/geography/all")
    public @ResponseBody List<Object[]> geographyAll() {		
    	return logEntitiesRepository.downloadsGeography(null, null);
    }
    
	@RequestMapping(value = "/{logType}/geography/all")
    public @ResponseBody List<Object[]> geographyAll(@PathVariable LogType logType) {		
    	return logEntitiesRepository.downloadsGeography(logType, null);
    }
    
	@RequestMapping(value = "/{logType}/{name}/geography/all")
    public @ResponseBody List<Object[]> geographyAll(@PathVariable LogType logType, 
    		@PathVariable String name) {		
    	return logEntitiesRepository.downloadsGeography(logType, name);
    }    
    
 
	@RequestMapping(value = "/geography/country/{code}")
    public @ResponseBody List<Object[]> geographyCountry(@PathVariable String code) {		
    	return logEntitiesRepository.downloadsCountry(code, null, null);
    }
    
	@RequestMapping(value = "/{logType}/geography/country/{code}")
    public @ResponseBody List<Object[]> geographyCountry(@PathVariable LogType logType,
    		@PathVariable String code) {		
    	return logEntitiesRepository.downloadsCountry(code, logType, null);
    }
    
	@RequestMapping(value = "/{logType}/{name}/geography/country/{code}")
    public @ResponseBody List<Object[]> geographyCountry(@PathVariable LogType logType, 
    		@PathVariable String name, @PathVariable String code) {		
    	return logEntitiesRepository.downloadsCountry(code, logType, name);
    }
}