package cpath.webservice;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import cpath.config.CPathSettings;

@Controller
public class AdminController extends BasicController {
	private static final Logger log = LoggerFactory.getLogger(AdminController.class);
	
	private final CPathSettings cpath;
	
	//TODO add option to toggle net.sf.ehcache.disabled=true/false system option?
	
	public AdminController() {
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
    		@RequestParam(required=false) String proxy,
    		@RequestParam(required=false) String debug) {

    	cpath.setAdminEnabled("on".equals(admin));
   		cpath.setProxyModelEnabled("on".equals(proxy));
   		cpath.setDebugEnabled("on".equals(debug));
    	
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
		
    	String path = cpath.homeDir(); 
    	
    	Map<String,String> files = files(path, null);

    	model.addAttribute("files", files.entrySet());
		
		return "homedir";
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
    			List<Object[]> dl = logEntitiesRepository.downloadsWorld(null, name);
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
    				sb.append("; mostly from: ")
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

    @RequestMapping("/robots.txt")
    public String robots() {
    	return "robots";
    }
    
    @RequestMapping("/tests")
    public String tests() {
    	return "tests";
    }
    
	/*
	 * Recursively gets the sorted filename->size map
	 * from the cpath2 home and dir and 'data' sub-directory.
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
	
}
