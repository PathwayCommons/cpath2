package cpath.webservice;

import java.io.IOException;
import java.util.*;

import javax.servlet.http.HttpServletRequest;


import cpath.config.CPathSettings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;



/**
 * 
 * @author rodche
 */
@Controller
public class LogStatsController extends BasicController {
	private static final Logger log = LoggerFactory.getLogger(LogStatsController.class);   
	
    
    /**
     * Makes current cpath2 instance properies 
     * available to all (JSP) views.
     * @return
     */
    @ModelAttribute("cpath")
    public CPathSettings instance() {
    	return CPathSettings.getInstance();
    }
        
    /* 
     * As this controller class is mapped to all cpath2 servlets 
     * (i.e., - to those associated with  /*, *.json, and *.html paths via the web.xml),
     * we have to avoid ambiguous request mappings and also 
     * use explicit redirects to .html methods if needed
     * (i.e, if a method is not supposed to return xml/json objects too)
     */  
    
    @RequestMapping("/stats")
    public String home() {
    	return "stats";
    }	    


    //TODO add several mappings for timeline and geo json results
    
    
	@RequestMapping(value = "/logs/timeline")
    public @ResponseBody Map<String,List<Object[]>> timeline(Model model, HttpServletRequest request) 
    		throws IOException 
    {		
    	return logEntitiesRepository.downloadsTimeline();
    }
	
	@RequestMapping(value = "/logs/stats.html")
    public @ResponseBody Map<String,List<Object[]>> stats(Model model, HttpServletRequest request) 
    		throws IOException 
    {		
    	return logEntitiesRepository.downloadsTimeline();
    }
	
	@RequestMapping(value = "/logs/timeline/$")
    public @ResponseBody Map<String,List<Object[]>> timelineForTypeAndName(Model model, HttpServletRequest request) 
    		throws IOException 
    {		
    	return logEntitiesRepository.downloadsTimeline();
    }
 
}