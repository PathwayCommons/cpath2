package cpath.webservice;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;

import cpath.config.CPathSettings;
import cpath.dao.MetadataDAO;
import cpath.service.Status;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.PathwayData;
import cpath.warehouse.beans.Mapping;

import org.apache.commons.io.FileUtils;
import org.biopax.validator.api.beans.ValidatorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


/**
 * 
 * @author rodche
 */
@Controller
public class MetadataController extends BasicController
{
    
	private static final Logger log = LoggerFactory.getLogger(MetadataController.class);   
	
    private final MetadataDAO service; // main PC db access
    
    public MetadataController(MetadataDAO service) {
		this.service = service;
	}
    
    
    /**
     * Makes current cpath2 instance properies 
     * available to all (JSP) views.
     * @return
     */
    @ModelAttribute("cpath")
    public CPathSettings instance() {
    	return CPathSettings.getInstance();
    }
        
    /* 
     * As this controller class is mapped to all cpath2 servlets 
     * (i.e., - to those associated with  /*, *.json, and *.html paths via the web.xml),
     * we have to avoid ambiguous request mappings and also 
     * use explicit redirects to .html methods if needed
     * (i.e, if a method is not supposed to return xml/json objects too)
     */  
    
    
    //important: home.html (there are several controllers, and /home would create a circular redirects...)
    @RequestMapping("/home.html")
    public String home(Model model) {
    	return "home";
    }	    
 
    
    @RequestMapping("/metadata/validations") // returns xml or json
    public @ResponseBody List<ValInfo> queryForValidationInfo() 
    {
		log.debug("Query for all validations summary (xml/json)");
    	return validationInfo();
    }


	@RequestMapping("/metadata/validations.html") //a JSP view
    public String queryForValidationInfoHtml(Model model) 
    {
		log.debug("Query for all validations summary (html)");
    	
		//get the list of POJOs:
    	model.addAttribute("providers", validationInfo());
    	
    	return "validations";
    }
 
    
    @RequestMapping("/metadata/validations/{identifier}.html") //a JSP view
    public String queryForValidation(
    		@PathVariable String identifier, Model model) 
    {
		log.debug("Getting a validation report (html) for:" 
				+ identifier);

    	ValidatorResponse body = service.validationReport(identifier, null);
		model.addAttribute("response", body);
		
		return "validation";
    }

    
    // returns XML or Json 
    @RequestMapping("/metadata/validations/{identifier}")
    public @ResponseBody ValidatorResponse queryForValidation(
    		@PathVariable String identifier) 
    {
		log.debug("Getting complete validation report (XML/Json) for: " 
			+ identifier);
		
    	return service.validationReport(identifier, null);
    } 
    
    
    // returns XML or Json 
    @RequestMapping("/metadata/validations/{identifier}/{file}")
    public @ResponseBody ValidatorResponse queryForValidation(
    		@PathVariable String identifier, @PathVariable String file) 
    {
		log.debug("Getting validation report (XML/Json) for: " 
			+ identifier + " (" + file + ")");
    	
    	return service.validationReport(identifier, file);
    }
       
    
    @RequestMapping("/metadata/validations/{identifier}/{file}.html") //a JSP view
    public String queryForValidationByProviderAndFile(
    		@PathVariable String identifier, @PathVariable String file, Model model) 
    {
		log.debug("Getting a validation report (html) for:" 
				+ identifier + " (" + file + ")");

    	ValidatorResponse body = service.validationReport(identifier, file);
		model.addAttribute("response", body);
		
		return "validation";
    }
	
    
    @RequestMapping(value = "/metadata/logo/{identifier}")
    public  @ResponseBody byte[] queryForLogo(@PathVariable String identifier) 
    		throws IOException {
    	
    	Metadata ds = service.getMetadataByIdentifier(identifier);
    	byte[] bytes = null;
    	
    	if(ds != null) {
    		bytes = ds.getIcon();
			BufferedImage bufferedImage = ImageIO
				.read(new ByteArrayInputStream(bytes));
			
			//resize (originals are around 125X60)
			bufferedImage = scaleImage(bufferedImage, 100, 50, null);
						
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			ImageIO.write(bufferedImage, "gif", byteArrayOutputStream);
			bytes = byteArrayOutputStream.toByteArray();
		}
        
        return bytes;
    }

    
    @RequestMapping(value = "/favicon.ico")
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
			errorResponse(Status.INTERNAL_ERROR, 
				"Failed to load icon image from " +  cpathLogoUrl, response);
			log.error("Failed to load icon image from " +  cpathLogoUrl, e);
		}

//		if(iconData == null || iconData.length == 0)
//			errorResponse(Status.INTERNAL_ERROR, 
//					"No icon image available from " +  cpathLogoUrl, response);
//		else 
		
        return iconData;
    }
    
    // to return a xml or json data http response
    @RequestMapping(value = "/metadata/datasources")
    public  @ResponseBody List<MetInfo> queryForDatasources() {
		log.debug("Getting pathway datasources info.");
    	
		List<MetInfo> list = new ArrayList<MetInfo>();
		
		for(Metadata m : service.getAllMetadata()) {
			MetInfo mi = new MetInfo();
			mi.setIdentifier(m.getIdentifier());
			mi.setDescription(m.getDescription());
			mi.setIcon(m.getIcon());
			mi.setName(m.getName());
			mi.setType(m.getType().name());
			mi.setNotPathwaydata(m.getType().isNotPathwayData());
			mi.setUri(m.getUri());
			mi.setUrlToHomepage(m.getUrlToHomepage());
			mi.getCounts().add(m.getNumPathways());
			mi.getCounts().add(m.getNumInteractions());
			mi.getCounts().add(m.getNumPhysicalEntities());
			list.add(mi);
		}
		
    	return list;
    }
    
    
    @RequestMapping(value = "/downloads.html")
    public String downloads(Model model) {
    	// get the sorted list of files to be shared on the web
    	String path = CPathSettings.downloadsDir(); 
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
    

    @RequestMapping(value="/idmapping")
    public @ResponseBody Map<String, String> search(@RequestParam String[] id, 
    		HttpServletResponse response) throws IOException
    {		
			log.debug("/idmapping called for: " + Arrays.toString(id));
			if(id == null || id.length == 0) {
				errorResponse(Status.NO_RESULTS_FOUND, "No ID(s) specified.", response);
				return null;
			}
			
			Map<String, String> res = new TreeMap<String, String>();

			for(String i : id) {							
				Set<String> im = service.mapIdentifier(i, Mapping.Type.UNIPROT, null);
				if(im.isEmpty())
					im = service.mapIdentifier(i, Mapping.Type.CHEBI, null);
				
				if(im == null) {
					res.put(i, null);
				} else {
					for(String ac : im)
						res.put(i, ac);
				}			
			}		

			return res;
	}
    
    
    
    private List<ValInfo> validationInfo() {
    	final List<ValInfo> list = new ArrayList<ValInfo>();
    	
		for(Metadata m : service.getAllMetadata()) {
			if(m.getType().isNotPathwayData())
				continue;
			
			ValInfo vi = new ValInfo();
			vi.setIdentifier(m.getIdentifier());
			
			for(PathwayData pd : m.getPathwayData())
				vi.getFiles().put(pd.getFilename(), pd + "; " + pd.status() + ")");
			
			list.add(vi);
		}
    		
		return list;
	}
    
    
    /**
     * A POJO for a JSP view (validations).
     * 
     */
    public static final class ValInfo {
    	String identifier;
    	//filename to status/description map
    	Map<String,String> files;
    	
    	public ValInfo() {
			files = new TreeMap<String,String>();
		}
    	
    	public String getIdentifier() {
			return identifier;
		}
    	public void setIdentifier(String identifier) {
			this.identifier = identifier;
		}
    	
    	public Map<String, String> getFiles() {
			return files;
		}
    	public void setFiles(Map<String, String> files) {
			this.files = files;
		}
    }
    
    /**
     * A POJO for a JSON view (datasources).
     * 
     */   
    public static final class MetInfo {
    	String type;
    	String urlToHomepage;
    	String uri;
    	String description;
    	byte[] icon;
    	String identifier;
    	List<String> name;
    	List<Integer> counts;
    	boolean notPathwaydata;
    	
    	public MetInfo() {
			counts = new ArrayList<Integer>(3);
		}
    	
    	public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}

		public String getUrlToHomepage() {
			return urlToHomepage;
		}
		public void setUrlToHomepage(String urlToHomepage) {
			this.urlToHomepage = urlToHomepage;
		}

		public String getUri() {
			return uri;
		}
		public void setUri(String uri) {
			this.uri = uri;
		}

		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}

		public byte[] getIcon() {
			return icon;
		}
		public void setIcon(byte[] icon) {
			this.icon = icon;
		}

		public String getIdentifier() {
			return identifier;
		}
		public void setIdentifier(String identifier) {
			this.identifier = identifier;
		}

		public List<String> getName() {
			return name;
		}
		public void setName(List<String> name) {
			this.name = name;
		}

		public List<Integer> getCounts() {
			return counts;
		}
		public void setCounts(List<Integer> counts) {
			this.counts = counts;
		}  
		
		public boolean isNotPathwaydata() {
			return notPathwaydata;
		}
		public void setNotPathwaydata(boolean pathwayData) {
			this.notPathwaydata = pathwayData;
		}
    }
    
}