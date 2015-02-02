package cpath.webservice;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

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
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;


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
 
    
//    @RequestMapping("/validations/{identifier}.html") //a JSP view
//    public String queryForValidation(
//    		@PathVariable String identifier, Model model, HttpServletRequest request) 
//    {
//
//    	ValidatorResponse body = service.validationReport(identifier, null);
//		model.addAttribute("response", body);
//		
//		return "validation";
//    }
    
//    @RequestMapping("/validations/{identifier}/{file}.html") //a JSP view
//    public String queryForValidationByProviderAndFile(
//    		@PathVariable String identifier, @PathVariable String file, 
//    		Model model, HttpServletRequest request) 
//    {
//    	ValidatorResponse body = service.validationReport(identifier, file);
//		model.addAttribute("response", body);
//		
//		return "validation";
//    }    
       
//    @RequestMapping("/metadata/validations/{identifier}")
//    public @ResponseBody ValidatorResponse queryForValidation(
//    		@PathVariable String identifier, HttpServletRequest request) 
//    {	
//    	return service.validationReport(identifier, null);
//    }
    
    // returns XML or Json 
    @RequestMapping("/metadata/validations/{identifier}/{file}")
    public @ResponseBody ValidatorResponse queryForValidation(
    		@PathVariable String identifier, @PathVariable String file,
    		HttpServletRequest request) 
    {	
    	return service.validationReport(identifier, file);
    }
       
    
    @RequestMapping("/metadata/logo/{identifier}")
    public  @ResponseBody byte[] queryForLogo(@PathVariable String identifier) 
    		throws IOException 
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
				m.setNumAccessed(service.log().downloads(m.standardName()));
				m.setUploaded((new File(m.getDataArchiveName()).exists()));
				m.setPremerged(!m.getContent().isEmpty());
				m.setNumUniqueIps(service.log().uniqueIps(m.standardName()));
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
			m.setNumAccessed(service.log().downloads(m.standardName()));
			m.setUploaded((new File(m.getDataArchiveName()).exists()));
			m.setPremerged(!m.getContent().isEmpty());
			m.setNumUniqueIps( service.log().uniqueIps(m.standardName()));
		}
		
    	return m;
    }    
    
    
    // Requests that begin with '/admin' can be only run by authorized users (see: Spring security-config.xml).
    
    @RequestMapping(value = "/admin/datasources", consumes="application/json", method = RequestMethod.POST)
    public void update(@RequestBody @Valid Metadata metadata, 
    		BindingResult bindingResult, HttpServletResponse response) throws IOException 
    {	
    	if(bindingResult != null &&  bindingResult.hasErrors()) {
    		log.error(Status.BAD_REQUEST.getErrorCode() + "; " +  
        			Status.BAD_REQUEST.getErrorMsg() + "; " + errorFromBindingResult(bindingResult));
    		response.sendError(Status.BAD_REQUEST.getErrorCode(), 
    			Status.BAD_REQUEST.getErrorMsg() + "; " + errorFromBindingResult(bindingResult));
    	}
    	
   		service.save(metadata);
    }

    
    @RequestMapping(value = "/admin/datasources", consumes="application/json", method = RequestMethod.PUT)
    public void put(@RequestBody @Valid Metadata metadata, 
    		BindingResult bindingResult, HttpServletResponse response) throws IOException 
    {	
    	if(bindingResult != null &&  bindingResult.hasErrors()) {
    		log.error(Status.BAD_REQUEST.getErrorCode() + "; " +  
        			Status.BAD_REQUEST.getErrorMsg() + "; " + errorFromBindingResult(bindingResult));
    		response.sendError(Status.BAD_REQUEST.getErrorCode(), 
    			Status.BAD_REQUEST.getErrorMsg() + "; " + errorFromBindingResult(bindingResult));
    	}
    	
    	Metadata existing = service.metadata().findByIdentifier(metadata.identifier);
    	if(existing == null) {
    		service.save(metadata);
    	} else {
    		response.sendError(Status.BAD_REQUEST.getErrorCode(), 
                "PUT failed: Metadata already exists for pk: " + metadata.identifier
                	+ " (use POST to update instead)");
    	}
    }
    
    @RequestMapping(value = "/admin/datasources/{identifier}", method = RequestMethod.DELETE)
    public void delete(@PathVariable String identifier, HttpServletResponse response) throws IOException 
    {	
    	Metadata existing = service.metadata().findByIdentifier(identifier);
    	if(existing != null) {
    		service.delete(existing);//clear files
    	} else {
    		response.sendError(Status.NO_RESULTS_FOUND.getErrorCode(), 
            	"DELETE failed: no Metadata record found for pk: " + identifier);
    	}
    }

    
    //Upload a data archive
    @RequestMapping(value = "/admin/datasources/{identifier}/file", method = RequestMethod.POST)
    public void uploadDataArchive(@PathVariable String identifier, MultipartHttpServletRequest multiRequest, 
    		HttpServletResponse response) throws IOException
    {	    	
    	Metadata m = service.metadata().findByIdentifier(identifier);
    	if(m==null) {
    		response.sendError(Status.NO_RESULTS_FOUND.getErrorCode(), Status.NO_RESULTS_FOUND.getErrorMsg() + 
        		"; Metadata object with identifier: " + identifier + " not found.");
    	}
    	
		Map<String, MultipartFile> files = multiRequest.getFileMap();
		Assert.state(!files.isEmpty(), "No files to validate");
		String filename = files.keySet().iterator().next();
		MultipartFile file = files.get(filename);
		String origFilename = file.getOriginalFilename();			
		if(file.getBytes().length==0 || filename==null || "".equals(filename) || !origFilename.endsWith(".zip")) {
			log.error("uploadDataArchive(), empty data file or null: " + origFilename);
			response.sendError(Status.BAD_REQUEST.getErrorCode(), 
	            	"File (" + origFilename + ") UPLOAD failed; id:" + identifier);
		} else {
			//create or update the input source data file (must be ZIP archive!)
			CPathUtils.write(file.getBytes(), m.getDataArchiveName());
			log.info("uploadDataArchive(), saved uploaded file:" 
					+ origFilename + " as " + m.getDataArchiveName());
		}
    }

    
    @RequestMapping("/idmapping")
    public @ResponseBody Map<String, String> idMapping(@RequestParam String[] id, 
    		HttpServletRequest request, HttpServletResponse response) throws IOException
    {			
    	//log events: command, format
    	Set<LogEvent> events = new HashSet<LogEvent>();
    	events.add(LogEvent.IDMAPPING);
    	events.add(LogEvent.FORMAT_OTHER);

    	if(id == null || id.length == 0) {
    		errorResponse(Status.NO_RESULTS_FOUND, "No ID(s) specified.", 
    				request, response, events);
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
        
}