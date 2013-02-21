package cpath.webservice;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.imageio.ImageIO;

//import cpath.service.jaxb.*;
import cpath.config.CPathSettings;
import cpath.warehouse.MetadataDAO;
import cpath.warehouse.beans.Metadata;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.validator.api.beans.ValidatorResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


/**
 * 
 * @author rodche
 */
@Controller
public class MetadataController extends BasicController 
{
    
	private static final Log log = LogFactory.getLog(MetadataController.class);    
	
    private MetadataDAO service; // main PC db access
	
    public MetadataController(MetadataDAO service) {
		this.service = service;
	}
    
    @RequestMapping("/validation/{metadataId}/files.html") //a JSP view
    public String queryForValidations(@PathVariable String metadataId, Model model) 
    {
		log.debug("Query - all validations (separate files) by datasource: " + metadataId);
    	
    	Map<Integer,String> result = service.getPathwayDataInfo(metadataId);
    	
    	model.addAttribute("identifier", metadataId);
    	if(result != null)
    		model.addAttribute("results", result.entrySet());
    	
    	return "validations";
    }
    
    
    @RequestMapping("/validation/{key}")
    public @ResponseBody ValidatorResponse queryForValidation(@PathVariable String key) 
    {
		log.debug("Getting a validation report for: " + key);
    	
    	//distinguish between a metadata and pathwayData primary key cases:
    	try {
    		Integer pk = Integer.parseInt(key);
    		return service.getValidationReport(pk);
    	} catch (NumberFormatException e) {}
    	
    	return service.getValidationReport(key);
    }

    
    @RequestMapping("/validation/{key}.html") //a JSP view
    public String queryForValidation(@PathVariable String key, Model model) 
    {
		log.debug("Getting a validation report, as html, for:" + key);

    	ValidatorResponse body = queryForValidation(key);
		model.addAttribute("response", body);
		
		return "validationSummary";
    }
	
    
    @RequestMapping(value = "/logo/{identifier}")
    public  @ResponseBody byte[] queryForLogo(@PathVariable String identifier) throws IOException {
    	// try to get the metadata record by id:
    	Metadata ds = service.getMetadataByIdentifier(identifier);
    	byte[] bytes = null;
    	
    	if(ds != null) {
    		bytes = ds.getIcon();
    	} else {
    		for(Metadata m : service.getAllMetadata())
    			if(m.getIdentifier().equalsIgnoreCase(identifier)) {
    				bytes = m.getIcon();
    				break;
    			}
    	}
    	
		if (bytes != null) {
			BufferedImage bufferedImage = ImageIO
					.read(new ByteArrayInputStream(bytes));
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			ImageIO.write(bufferedImage, "gif", byteArrayOutputStream);
			bytes = byteArrayOutputStream.toByteArray();
		}
        
        return bytes;
    }
       
    
    @RequestMapping(value = "/metadata")
    public  @ResponseBody Collection<Metadata> queryPathwayMetadata() {
		log.debug("Getting pathway type Metadata.");
    	
    	return service.getAllMetadata();
    }
    
    
    @RequestMapping("/downloads")
    public String redirectToDownloadsHtmlPage() {
    	return "redirect:downloads.html";
    }
    
    @RequestMapping(value = "/downloads.html")
    public String downloads(Model model) {
    	// get the sorted list of files to be shared on the web
    	String path = CPathSettings.getHomeDir() 
    		+ File.separator + CPathSettings.DOWNLOADS_SUBDIR; 
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
		
		return "downloads";
    }
    
}