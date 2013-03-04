package cpath.webservice;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import cpath.config.CPathSettings;
import cpath.warehouse.MetadataDAO;

@Controller
public class CPathAdminController {

	private final MetadataDAO metadataDAO;
	
	public CPathAdminController(MetadataDAO metadataDAO) {
		this.metadataDAO = metadataDAO;
	}
	
	
    @ModelAttribute("maintenanceModeEnabled")
    public boolean isMaintenanceModeEnabled() {
    	return CPathSettings.isMaintenanceEnabled();
    }
    
    
    @RequestMapping("/admin")
    public String adminPage() {
    	return "admin";
    }
    
	
	@RequestMapping(value = "/admin/data")
    public String data(Model model) {
    	// get the sorted list of files
    	String path = CPathSettings.localDataDir(); 
    	File[] list = new File(path).listFiles();
    	
    	Map<String,String> files = new TreeMap<String,String>();
    	
    	for(int i = 0 ; i < list.length ; i++) {
    		File f = list[i];
    		String name = f.getName();
    		long size = f.length();
    		if(!name.startsWith("."))
    			files.put(name, FileUtils.byteCountToDisplaySize(size));
    	}
    	model.addAttribute("files", files.entrySet());
		
		return "data";
    }	
	
	@RequestMapping(value = "/admin/tmp")
    public String tmp(Model model) {
    	// get the sorted list of files
    	String path = CPathSettings.tmpDir(); 
    	File[] list = new File(path).listFiles();
    	
    	Map<String,String> files = new TreeMap<String,String>();
    	
    	for(int i = 0 ; i < list.length ; i++) {
    		File f = list[i];
    		String name = f.getName();
    		long size = f.length();
    		if(!name.startsWith("."))
    			files.put(name, FileUtils.byteCountToDisplaySize(size));
    	}
    	model.addAttribute("files", files.entrySet());
		
		return "tmp";
    }
		
	
	@RequestMapping(value = "/admin/homedir")
    public String homedir(Model model) {
    	// get the sorted list of files
    	String path = CPathSettings.homeDir(); 
    	File[] list = new File(path).listFiles();
    	
    	Map<String,String> files = new TreeMap<String,String>();
    	
    	for(int i = 0 ; i < list.length ; i++) {
    		File f = list[i];
    		String name = f.getName();
    		long size = f.length();
    		if(!name.startsWith("."))
    			files.put(name, FileUtils.byteCountToDisplaySize(size));
    	}
    	model.addAttribute("files", files.entrySet());
		
		return "homedir";
    }
}
