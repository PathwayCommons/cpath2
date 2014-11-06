package cpath.webservice;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import cpath.config.CPathSettings;
import cpath.jpa.LogType;

@Controller
public class PagesController extends BasicController {
	private static final Logger log = LoggerFactory.getLogger(PagesController.class);
	
	private final CPathSettings cpath;
	
	
	public PagesController() {
		cpath = CPathSettings.getInstance();
	}
	
	
    @ModelAttribute("cpath")
    public CPathSettings instance() {
    	return cpath;
    }

    @RequestMapping("/home")
    public String home() {
    	return "home";
    }	    
    
    @RequestMapping("/formats")
    public String getOutputFormatsDescr() 
    {
    	return "formats";
    }

    
    @RequestMapping("/datasources")
    public String datasources() {
    	return "datasources";
    }     
    
    
    @RequestMapping(value="/admin", method=RequestMethod.GET)
    public String adminPage() {
    	return "admin";
    }
    
    @RequestMapping(value="/admin", method=RequestMethod.POST)
    public String adminPageAction(
    		@RequestParam(required=false) String admin, 
    		@RequestParam(required=false) String debug) {

    	cpath.setAdminEnabled("on".equals(admin));
   		cpath.setDebugEnabled("on".equals(debug));
    	
//   		if(!cpath.isAdminEnabled())
//   			service.init();
   		
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

	@RequestMapping("/admin/homedir")
    public String homedir(Model model, HttpServletRequest request) {
		
    	String path = cpath.homeDir(); 
    	
    	//find/list all files/dirs in the homedir, but traverse only into "data" subdir
    	Map<String,String> files = files(path, null, true, Collections.singleton("data"));

    	model.addAttribute("files", files.entrySet());
		
		return "homedir";
    }

    @RequestMapping("/datadir")
    public String data(Model model) {
    	String path = cpath.dataDir(); 
    	
    	//find/list all files in the datadir, but traverse into every provider's subdir.
    	Map<String,String> files = files(path, null, false, null);

    	model.addAttribute("files", files.entrySet());
    	
    	return "datadir";
    }	
	
    @RequestMapping("/downloads")
    public String downloads(Model model, HttpServletRequest request) {

    	// get the sorted list of files to be shared on the web
    	String path = CPathSettings.getInstance().downloadsDir(); 
    	File[] list = new File(path).listFiles();
    	
    	Map<String,String> files = new TreeMap<String,String>();
    	
    	for(int i = 0 ; i < list.length ; i++) {
    		File f = list[i];
    		String name = f.getName();
    		long size = f.length();
    		
    		if(!name.startsWith(".")) {
    			StringBuilder sb = new StringBuilder();
    			sb.append("size: ").append(FileUtils.byteCountToDisplaySize(size));
    			List<Object[]> dl = service.log().downloadsWorld(null, name);
    			String topCountry = null;
    			long topCount = 0;
    			long total = 0;
    			Iterator<Object[]> it = dl.iterator();
    			it.next(); //skip title line
    			while(it.hasNext()) {
    				Object[] a = it.next();
    				long count = (Long) a[1];
    				total += count;
    				if(count > topCount) {
    					topCount = count;
    					topCountry = (String) a[0];
    				}   					
    			}
    			
    			sb.append("; downloads: ").append(total);
    			if(topCount > 0) {
    				sb.append(", mostly from ")
    				.append((topCountry != null && !topCountry.isEmpty()) 
    						? topCountry : "Local/Unknown")
    				.append(" [").append(topCount).append("]");
    			}
    			
    			files.put(name, sb.toString());
    		}
    	}
    	
    	model.addAttribute("files", files.entrySet());
		
		return "downloads";
    }	
	
    // Access Log UI 
    
    @RequestMapping("/log")
    public String log() {
    	return allStats();
    }
    
    @RequestMapping("/log/stats")
    public String allStats() {
    	return "redirect:/log/TOTAL/stats";
    }
        
    @RequestMapping("/log/{logType}/stats")
    public String statsByType(Model model, @PathVariable LogType logType) {
    	model.addAttribute("summary_for", "Category: " + logType);
    	model.addAttribute("current", "/log/"+logType.toString()+"/stats");
    	return "stats";
    }
    
    @RequestMapping("/log/{logType}/{name}/stats")
    public String statsByType(Model model, @PathVariable LogType logType,
    		@PathVariable String name) {
    	model.addAttribute("summary_for", "Category: " + logType + ", name: " + name);
    	model.addAttribute("current", "/log/"+logType.toString()+"/"+name+"/stats");
    	return "stats";
    } 

    @RequestMapping("/log/ips")
    public String allIps() {
    	return "redirect:/log/TOTAL/ips";
    }
    
    @RequestMapping("/log/{logType}/ips")
    public String ipsByType(Model model, @PathVariable LogType logType) {
    	model.addAttribute("summary_for", "Category: " + logType);
    	model.addAttribute("current", "/log/"+logType.toString()+"/ips");
    	return "ips";
    }
    
    @RequestMapping("/log/{logType}/{name}/ips")
    public String ipsByType(Model model, @PathVariable LogType logType,
    		@PathVariable String name) {
    	model.addAttribute("summary_for", "Category: " + logType + ", name: " + name);
    	model.addAttribute("current", "/log/"+logType.toString()+"/"+name+"/ips");
    	return "ips";
    }    
    

    // The Web App (AngularJS, rich HTML5 portal)
    @RequestMapping("/view")
    public String view() {
    	return "view";
    }
    
    // OTHER resources
    
    @RequestMapping("/favicon.ico")
    public  @ResponseBody byte[] icon(HttpServletResponse response) 
    		throws IOException {
    	
    	String cpathLogoUrl = CPathSettings.getInstance().getLogoUrl();
    	
		byte[] iconData = null;

		try {
			BufferedImage image = ImageIO.read(new URL(cpathLogoUrl));
			if(image != null) {
				image = scaleImage(image, 16, 16, null);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(image, "gif", baos);
				baos.flush();
				iconData = baos.toByteArray();
			}
		} catch (IOException e) {
			log.error("Failed to load icon image from " +  cpathLogoUrl, e);
		}
		
        return iconData;
    }
    
    @RequestMapping("/tests")
    public String tests() {
    	return "tests";
    }
    
	/**
	 * Recursively gets the sorted filename->size map
	 * from the cpath2 home and dir and 'data' sub-directory.
	 * 
	 * TODO consider using a Tree object (set of nodes) in the future
	 * 
     * @param path
     * @param relativePath
     * @param showDirNames
     * @param goIntoDirs expand (list files in) the specified sub-directories, or all - if null.
     * @return
     */
	private Map<String, String> files(String path, String relativePath, 
			boolean showDirNames, Collection<String> goIntoDirs) 
	{
		Map<String,String> files = new TreeMap<String,String>();
		
		String fullPath = (relativePath == null) ? path 
				: path + File.separator + relativePath;		
		File[] list = new File(fullPath).listFiles();
		
    	for(int i = 0 ; i < list.length ; i++) {
    		File f = list[i];
    		String name = f.getName();
    		
    		if(name.startsWith(".")) 
    			continue; //skip system/hidden
    		
    		//add curr. rel. path to this filename
    		name = (relativePath == null) ? name 
					: relativePath + File.separator + name;
    		
    		if(f.isDirectory() && (goIntoDirs==null || goIntoDirs.contains(name))) { 
    			//deep traverse into the data dir. only
    			files.putAll(files(path, name, showDirNames, goIntoDirs));
    		} else {	
    			String size =  (f.isDirectory()) ? "directory"
    					: FileUtils.byteCountToDisplaySize(f.length());
    			
    			if(!f.isDirectory() || showDirNames)
    				files.put(name, size);
    		}
    	}
    	
    	return files;
	}
	
}
