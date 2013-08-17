package cpath.webservice;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import cpath.config.CPathSettings;
import cpath.dao.CPathUtils;
import cpath.dao.MetadataDAO;

@Controller
public class CPathAdminController extends BasicController {

	private final MetadataDAO metadataDAO;
	
	public CPathAdminController(MetadataDAO metadataDAO) {
		this.metadataDAO = metadataDAO;
	}
	
	
    @ModelAttribute("cpath")
    public CPathSettings instance() {
    	return CPathSettings.getInstance();
    }
    
    @RequestMapping(value="/admin", method=RequestMethod.GET)
    public String adminPage() {
    	return "admin";
    }
    
    @RequestMapping(value="/admin", method=RequestMethod.POST)
    public String adminPageAction(@RequestParam String toggle, HttpServletRequest request) {
    	logHttpRequest(request);
    	
    	if(toggle != null && !toggle.isEmpty()) {
    		CPathSettings cfg = CPathSettings.getInstance();
    		cfg.setAdminEnabled(!cfg.isAdminEnabled());
    	}
    	
    	return "admin";
    }
    
    @RequestMapping("/login.html")
    public String login() {
    	return "login";
    }
    
    @RequestMapping("/denied.html")
    public String denied() {
    	return "denied";
    }
       
    @RequestMapping("/error.html")
    public String error() {
    	return "error";
    }
    
	@RequestMapping(value = "/admin/data")
    public String data(Model model, HttpServletRequest request) {
		logHttpRequest(request);
		
    	String path = CPathSettings.dataDir(); 
    	
    	Map<String,String> files = files(path);

    	model.addAttribute("files", files.entrySet());
		
		return "data";
    }	
	
	@RequestMapping(value = "/admin/tmp")
    public String tmp(Model model, HttpServletRequest request) {
		logHttpRequest(request);
		
    	String path = CPathSettings.tmpDir();
    	
    	Map<String,String> files = files(path);
    	
    	model.addAttribute("files", files.entrySet());
		
		return "tmp";
    }


	@RequestMapping(value = "/admin/homedir")
    public String homedir(Model model, HttpServletRequest request) {
		logHttpRequest(request);
		
    	String path = CPathSettings.homeDir(); 
    	
    	Map<String,String> files = files(path);

    	model.addAttribute("files", files.entrySet());
		
		return "homedir";
    }
	

	/*
	 * Given a directory path, gets a sorted  
	 * filename->size map of its content.
	 */
	private Map<String, String> files(String path) {
    	File[] list = new File(path).listFiles();
    	
    	Map<String,String> files = new TreeMap<String,String>();
    	
    	for(int i = 0 ; i < list.length ; i++) {
    		File f = list[i];
    		String name = f.getName();
    		long size = f.length();
    		if(!name.startsWith("."))
    			files.put(name, FileUtils.byteCountToDisplaySize(size));
    	}
    	
    	return files;
	}
	

	@RequestMapping(value = "/admin/stats", method=RequestMethod.GET)
    public String stats(HttpServletRequest request) throws IOException {
		logHttpRequest(request);    	
		return "stats";
    }
	
	@RequestMapping(value = "/admin/stats", method=RequestMethod.POST)
    public String stats(Model model, HttpServletRequest request) throws IOException {
		logHttpRequest(request);
		
    	// update (unique IP, cmd, datasource, etc.) counts from log files
    	Map<String,Integer> counts = CPathUtils.simpleStatsFromAccessLogs();
    	model.addAttribute("counts", counts.entrySet());
		
		return "stats";
    }
	
}
