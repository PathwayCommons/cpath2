package cpath.webservice;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cpath.config.CPathSettings;
import cpath.dao.CPathUtils;
import cpath.jpa.Content;
import cpath.jpa.LogEvent;
import cpath.jpa.Metadata;
import cpath.service.Status;
import cpath.webservice.args.binding.MetadataTypeEditor;

import org.biopax.validator.api.beans.ValidatorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
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
public class MetadataController extends BasicController {
    
	private static final Logger log = LoggerFactory.getLogger(MetadataController.class);   
	
    
	@InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Metadata.METADATA_TYPE.class, new MetadataTypeEditor());
	}
    
    
	//JSP Views
	
    /*
     * Makes current cpath2 instance properties 
     * available to the JSP views.
     * @return
     */
    @ModelAttribute("cpath")
    public CPathSettings instance() {
    	return CPathSettings.getInstance();
    }

    
	@RequestMapping("/validations")
    public String queryForValidationInfoHtml(Model model) 
    {
		//get the list of POJOs:
    	model.addAttribute("providers", validationInfo());
    	
    	return "validations";
    }
    
    // returns XML or Json 
    @RequestMapping("/metadata/validations/{identifier}/{file}")
    public @ResponseBody ValidatorResponse queryForValidation(
    		@PathVariable String identifier, @PathVariable String file,
    		HttpServletRequest request) 
    {	
    	return service.validationReport(identifier, file);
    }
       
    
    @RequestMapping("/metadata/logo/{identifier}")
    public  @ResponseBody byte[] queryForLogo(@PathVariable String identifier) throws IOException
    {	
    	Metadata ds = service.metadata().findByIdentifier(identifier);
    	byte[] bytes = null;
    	
    	if(ds != null) {
			BufferedImage bufferedImage = ImageIO
				.read(CPathUtils.LOADER.getResource(ds.getIconUrl()).getInputStream());
			
			//resize (originals are around 125X60)
			bufferedImage = scaleImage(bufferedImage, 100, 50, null);
						
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			ImageIO.write(bufferedImage, "gif", byteArrayOutputStream);
			bytes = byteArrayOutputStream.toByteArray();
		}
        
        return bytes;
    }

    
    // to return a xml or json data http response
    @RequestMapping("/metadata/datasources")
    public  @ResponseBody List<Metadata> queryForDatasources() {
		log.debug("Getting pathway datasources info.");
    	//pathway/interaction data sources
		List<Metadata> ds = new ArrayList<Metadata>();
		//warehouse data sources
		List<Metadata> wh = new ArrayList<Metadata>();
		
		for(Metadata m : service.metadata().findAll()) {
			//set dynamic extra fields
			if(m.isNotPathwayData()) {
				m.setUploaded((new File(m.getDataArchiveName()).exists()));
				wh.add(m);
			} else {		
//don't query for logs (web pages are slow)
//				m.setNumAccessed(service.log().downloads(m.standardName()));
//				m.setNumUniqueIps(service.log().uniqueIps(m.standardName()));				
				m.setUploaded((new File(m.getDataArchiveName()).exists()));
				m.setPremerged(!m.getContent().isEmpty());
				ds.add(m);
			}
		}
		
		//add warehouse data sources to the end of the list
		ds.addAll(wh);
		
    	return ds;
    }
    
    @RequestMapping("/metadata/datasources/{identifier}")
    public  @ResponseBody Metadata datasource(@PathVariable String identifier) {
		Metadata m = service.metadata().findByIdentifier(identifier);
		if(m==null)
			return null;
		
		//set dynamic extra fields
		if(m.isNotPathwayData()) {
			m.setUploaded((new File(m.getDataArchiveName()).exists()));
		} else {
			//don't query for logs (web pages are slow)
//			m.setNumAccessed(service.log().downloads(m.standardName()));
//			m.setNumUniqueIps( service.log().uniqueIps(m.standardName()));
			m.setUploaded((new File(m.getDataArchiveName()).exists()));
			m.setPremerged(!m.getContent().isEmpty());
		}
		
    	return m;
    }
    
    @RequestMapping("/idmapping")
    public @ResponseBody Map<String, String> idMapping(@RequestParam String[] id, 
    		HttpServletRequest request, HttpServletResponse response)
    {			
    	//log events: command, format
    	Set<LogEvent> events = new HashSet<LogEvent>();
    	events.add(LogEvent.IDMAPPING);

    	if(id == null || id.length == 0) {
    		errorResponse(Status.NO_RESULTS_FOUND, "No ID(s) specified.", request, response, events);
    		return null;
    	}

    	Map<String, String> res = new TreeMap<String, String>();

    	for(String i : id) {							
    		Set<String> im = service.map(i);
    		if(im == null) {
    			res.put(i, null);
    		} else {
    			for(String ac : im)
    				res.put(i, ac);
    		}			
    	}		

    	//log to db (for usage reports)
    	service.log(events, clientIpAddress(request));

    	return res;
	}
 
    
    private List<ValInfo> validationInfo() {
    	final List<ValInfo> list = new ArrayList<ValInfo>();
    	
		for(Metadata m : service.metadata().findAll()) {
			if(m.isNotPathwayData())
				continue;
			
			ValInfo vi = new ValInfo();
			vi.setIdentifier(m.getIdentifier());
			
			for(Content pd : m.getContent())
				vi.getFiles().put(pd.getFilename(), pd.toString());
			
			list.add(vi);
		}
    		
		return list;
	}
    
    
    /**
     * A POJO for a JSP view (validations).
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
        
}