package cpath.webservice;

import java.io.File;
import java.util.*;

import javax.servlet.http.HttpServletRequest;

import cpath.config.CPathSettings;
import cpath.jpa.LogEvent;
import cpath.jpa.LogType;
import cpath.jpa.Metadata;
import cpath.webservice.args.binding.LogTypeEditor;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
	
	@RequestMapping("/{logType}/{name}/timelinecum")
    public @ResponseBody Map<String,List<Object[]>> timelinecum(@PathVariable LogType logType, @PathVariable String name) {		
    	return service.log().downloadsTimelineCum(logType, name);
    }    
    
    @RequestMapping("/{logType}/timelinecum")
    public @ResponseBody Map<String,List<Object[]>> timelinecum(@PathVariable LogType logType) {		
    	return service.log().downloadsTimelineCum(logType, null);
    }
	
	@RequestMapping("/timelinecum")
    public @ResponseBody Map<String,List<Object[]>> timelinecum() {		
    	return service.log().downloadsTimelineCum(LogType.TOTAL, null);
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
    
	/**
	 * Total no. requests minus errors.
	 * @return
	 */
	@RequestMapping("/totalok")
    public @ResponseBody Long totalRequests() {		
    	return service.log().totalRequests() - service.log().totalErrors();
    }
	
	/**
	 * Total no. unique client IPs.
	 * @return
	 */
	@RequestMapping("/totalip")
    public @ResponseBody Long totalUniqueIps() {		
    	return service.log().totalUniqueIps();
    }
	
	/*
	 * Access log summary - 
	 * a json object (map) that contains the list of rows ('data'),
	 * each row is object (map) with three columns (key-val pairs);
	 * this can be then read by a jQuery DataTable 
	 * via "ajaxSource" and "columns" parameters.
	 * 
	 * @return object
	 */
	@RequestMapping("/totals")
    public @ResponseBody Map<String, List<Map<String,?>>> totalTable() {	
		Map<String, List<Map<String,?>>> data = new HashMap<String,List<Map<String,?>>>();
		List<Map<String,?>> l = new ArrayList<Map<String,?>>();
		data.put("data", l); //there will be only one data entry (the list)
		
		Map<String, Object> m;
		Long totalPathways = 0L;
		Long totalInteractions = 0L;
		
		//also - for each provider -
		int numProviders = 0; //not warehouse data
		for(Metadata mtda : service.metadata().findAll()) {
			if(mtda.isNotPathwayData())
				continue; //skip warehouse data sources
			
			m = new HashMap<String, Object>();
			m.put("identifier", mtda.identifier);
			m.put("name", mtda.standardName());
			m.put("totalok", service.log().downloads(mtda.standardName()));//actually, incl. errors
			m.put("totalip", service.log().uniqueIps(mtda.standardName()));
			m.put("pathways", mtda.getNumPathways());
			m.put("interactions", mtda.getNumInteractions());
			l.add(m);
			totalPathways += mtda.getNumPathways();
			totalInteractions += mtda.getNumInteractions();			
			numProviders++;
		}

		// add total requests/IPs counts to the list
		m = new HashMap<String, Object>();
		m.put("identifier", LogType.TOTAL.description); //"All"
		m.put("name", "PC2 v" + CPathSettings.getInstance().getVersion() + " web service");
		m.put("datasources", numProviders);
		m.put("totalok", totalRequests());
		m.put("totalip", totalUniqueIps());
		m.put("pathways", totalPathways);
		m.put("interactions", totalInteractions);
		l.add(0, m);
		
		return data;
    }
	
	/*
	 * Access log summary for files in the downloads - a json object (map) that
	 * contains the list of rows ('data'), each row is object (map) with three
	 * columns (key-val pairs); this can be then read by a jQuery DataTable via
	 * "ajaxSource" and "columns" parameters
	 * 
	 * @return object
	 */
	@RequestMapping("/downloads")
	public @ResponseBody Map<String, List<Map<String, ?>>> downloads(
			Model model, HttpServletRequest request) {
		Map<String, List<Map<String, ?>>> data = new HashMap<String, List<Map<String, ?>>>();
		List<Map<String, ?>> l = new ArrayList<Map<String, ?>>();
		data.put("data", l); // there will be only one data entry (the list)

		Map<String, Object> m;

		// get the sorted list of files to be shared on the web
		String path = CPathSettings.getInstance().downloadsDir();
		File[] list = new File(path).listFiles();
		for (File f : list) {
			String name = f.getName();
			if (name.startsWith(".")) 
				continue; // skip sys files

			String size = FileUtils.byteCountToDisplaySize(f.length());
			long uips = service.log().uniqueIps(name);

			// find out from which country the file has been mostly requested;
			// count the top and total no. downloads
			List<Object[]> dl = service.log().downloadsWorld(null, name);
			String topc = null;
			long topdl = 0;
			long total = 0;
			Iterator<Object[]> it = dl.iterator();
			it.next(); // skip title line
			while (it.hasNext()) {
				Object[] a = it.next();
				long count = (Long) a[1];
				total += count;
				if (count > topdl) {
					topdl = count;
					topc = (String) a[0];
				}
			}

			m = new HashMap<String, Object>();
			m.put("name", name);
			m.put("size", size);
			m.put("downloads", total);// service.log().downloads(name));//incl. error requests
			m.put("uniqueips", uips);
			m.put("topdownloads", topdl);
			m.put("topcountry", topc);
			l.add(m);
		}

		return data;
	}
}