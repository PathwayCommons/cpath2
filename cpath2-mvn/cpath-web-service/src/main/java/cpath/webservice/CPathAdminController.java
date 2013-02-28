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
	
    @ModelAttribute("maintenanceMode")
    public String getMaintenanceModeMsgIfEnabled() {
    	if(CPathSettings.isMaintenanceModeEnabled())
    		return "Maintenance mode is enabled";
    	else 
    		return "";
    }
	
	
    @RequestMapping("/data")
    public String redirectToDownloadsHtmlPage() {
    	return "redirect:data.html";
    }	
	
	@RequestMapping(value = "/data.html")
    public String downloads(Model model) {
    	// get the sorted list of files
    	String path = CPathSettings.homeDir() 
    		+ File.separator + CPathSettings.DATA_SUBDIR; 
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
	
}
