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
public class AdminController extends BasicController {

	private final MetadataDAO metadataDAO;
	
	//TODO add CPathService field and controllers to set cpath2.proxy.model.enabled=true/false and load/unload the model in RAM
	
	//TODO add option to toggle net.sf.ehcache.disabled=true/false system option
	
	public AdminController(MetadataDAO metadataDAO) {
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
    
    @RequestMapping("/login")
    public String login() {
    	return "login";
    }
    
    @RequestMapping("/denied")
    public String denied() {
    	return "denied";
    }
       
    @RequestMapping("/error")
    public String error() {
    	return "error";
    }


	@RequestMapping(value = "/admin/homedir")
    public String homedir(Model model, HttpServletRequest request) {
		logHttpRequest(request);
		
    	String path = CPathSettings.homeDir(); 
    	
    	Map<String,String> files = files(path, null);

    	model.addAttribute("files", files.entrySet());
		
		return "homedir";
    }
	

	/*
	 * Recursively gets the sorted filename->size map
	 * from the cpath2 home dir (ignores hidden, tmp, 
	 * cache, and index directories).
	 * 
	 * TODO consider using a Tree object (set of nodes) in the future
	 */
	private Map<String, String> files(final String path, final String relativePath) {
		Map<String,String> files = new TreeMap<String,String>();
		
		String fullPath = (relativePath == null) ? path 
				: path + File.separator + relativePath;		
		File[] list = new File(fullPath).listFiles();
		
    	for(int i = 0 ; i < list.length ; i++) {
    		File f = list[i];
    		String name = f.getName();
    		
    		if(name.startsWith(".")) 
    			continue; //skip
    		
    		//add curr. rel. path to this filename
    		name = (relativePath == null) ? name 
					: relativePath + File.separator + name;
    		
    		if(f.isDirectory() && name.startsWith("data")) { 
    			//deep traverse into the data dir. only
    			files.putAll(files(path, name));
    		} else {	
    			String size =  (f.isDirectory()) ? "directory"
    					: FileUtils.byteCountToDisplaySize(f.length());  			
    			files.put(name, size);
    		}
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
