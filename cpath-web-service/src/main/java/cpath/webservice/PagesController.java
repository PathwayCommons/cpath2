package cpath.webservice;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import cpath.config.CPathSettings;

@Controller
public class PagesController extends BasicController {
	private static final Logger LOG = LoggerFactory.getLogger(PagesController.class);
	
	private final CPathSettings cpath;
	
	public PagesController() {
		cpath = CPathSettings.getInstance();
		LOG.info("Using CPATH2_HOME=" + cpath.homeDir());
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
    		@RequestParam(required=false) String debug,
    		@RequestParam(required=false) @DateTimeFormat(iso=ISO.DATE) Date logStartDate,
    		@RequestParam(required=false) @DateTimeFormat(iso=ISO.DATE) Date logEndDate)
    {

    	cpath.setAdminEnabled("on".equals(admin));
   		cpath.setDebugEnabled("on".equals(debug));

   		//check
   		if(logStartDate!=null && logEndDate!=null
   				&& logStartDate.compareTo(logEndDate) > 0) { 
   	   		LOG.error("adminPageAction, the log start date cannot be greater than end date (ignored)");
   	   		//TODO also show this error message on the page
   		} else { //update
   			try {
   				cpath.setLogStart(logStartDate);
   				cpath.setLogEnd(logEndDate);
   				LOG.info(String.format("adminPageAction, new default/global log timeline range: %s - %s.", 
   	   	   				cpath.getLogStart(), cpath.getLogEnd()));
   			} catch(Throwable e) {
   				LOG.error("adminPageAction, failed", e);
   			}
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
    			files.put(name, sb.toString());
    		}
    	}
    	
    	model.addAttribute("files", files.entrySet());
		
		return "downloads";
    }	

    // The Web App (AngularJS, rich HTML5 portal)
    @RequestMapping("/view")
    public String view() {
    	return "view";
    }
    
    // OTHER resources
    
    @RequestMapping("/favicon.ico")
    public  @ResponseBody byte[] icon() throws IOException {
    	
    	String cpathLogoUrl = CPathSettings.getInstance().getLogoUrl();
    	
		byte[] iconData = null;

		BufferedImage image = ImageIO.read(new URL(cpathLogoUrl));
		if(image != null) {
			image = scaleImage(image, 16, 16, null);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(image, "gif", baos);
			baos.flush();
			iconData = baos.toByteArray();
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
