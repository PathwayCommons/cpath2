package cpath.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import cpath.config.CPathSettings;

@Controller
public class PagesController extends BasicController
		implements ErrorController
{
	private static final Logger LOG = LoggerFactory.getLogger(PagesController.class);
	
	private final CPathSettings cpath;
	
	public PagesController() {
		cpath = CPathSettings.getInstance();
	}
	
	
    @ModelAttribute("cpath")
    public CPathSettings instance() {
    	return cpath;
    }

	@RequestMapping("/")
	public String wroot() {
		return "home";
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

	//handle error responses
    @RequestMapping("/error")
    public String error() {
    	return "error";
    }
	@Override
	public String getErrorPath() {
		return "/error";
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
		model.addAttribute("prefix", cpath.exportArchivePrefix());

		return "downloads";
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


	@RequestMapping("/robots.txt")
	public @ResponseBody String robots() {
		// deny robots access to logs, web services and data files,
		// but allow - to web page resources (css, js, images)
		return "User-agent: *\n" +
				"Disallow: /get\n" +
				"Disallow: /search\n" +
				"Disallow: /graph\n" +
				"Disallow: /top_pathways\n" +
				"Disallow: /traverse\n" +
				"Disallow: /archives\n" +
				"Disallow: /help\n" +
				"Disallow: /metadata\n";
	}
}
