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
import cpath.dao.LogUtils;
import cpath.dao.MetadataDAO;
import cpath.jpa.Geoloc;
import cpath.jpa.LogEvent;
import cpath.jpa.MappingsRepository;
import cpath.service.Status;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.Content;
import cpath.webservice.args.binding.MetadataTypeEditor;

import org.biopax.validator.api.beans.ValidatorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
	
    private final MetadataDAO metadataDAO;
    private final MappingsRepository mappingsRepository;
    
    @Autowired
    public MetadataController(MetadataDAO metadataDAO, MappingsRepository mappingsRepository) {
		this.metadataDAO = metadataDAO;
		this.mappingsRepository = mappingsRepository;
	}
    
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

    
	@RequestMapping("/metadata/validations")
    public String queryForValidationInfoHtml(Model model) 
    {
		log.debug("Query for all validations summary (html)");
    	
		//get the list of POJOs:
    	model.addAttribute("providers", validationInfo());
    	
    	return "validations";
    }
 
    
    @RequestMapping("/metadata/validations/{identifier}.html") //a JSP view
    public String queryForValidation(
    		@PathVariable String identifier, Model model, HttpServletRequest request) 
    {

    	ValidatorResponse body = metadataDAO.validationReport(identifier, null);
		model.addAttribute("response", body);
		
		return "validation";
    }
    
    @RequestMapping("/metadata/validations/{identifier}/{file}.html") //a JSP view
    public String queryForValidationByProviderAndFile(
    		@PathVariable String identifier, @PathVariable String file, 
    		Model model, HttpServletRequest request) 
    {
    	ValidatorResponse body = metadataDAO.validationReport(identifier, file);
		model.addAttribute("response", body);
		
		return "validation";
    }    

    
    // REST XML or Json web services
    
    @RequestMapping("/metadata/validations/{identifier}")
    public @ResponseBody ValidatorResponse queryForValidation(
    		@PathVariable String identifier, HttpServletRequest request) 
    {	
    	return metadataDAO.validationReport(identifier, null);
    } 
    
    
    // returns XML or Json 
    @RequestMapping("/metadata/validations/{identifier}/{file}")
    public @ResponseBody ValidatorResponse queryForValidation(
    		@PathVariable String identifier, @PathVariable String file,
    		HttpServletRequest request) 
    {	
    	return metadataDAO.validationReport(identifier, file);
    }
       
    
    @RequestMapping("/metadata/logo/{identifier}")
    public  @ResponseBody byte[] queryForLogo(@PathVariable String identifier) 
    		throws IOException 
    {	
    	Metadata ds = metadataDAO.getMetadataByIdentifier(identifier);
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
    	
		List<Metadata> list = metadataDAO.getAllMetadata();
		
		for(Metadata m : metadataDAO.getAllMetadata()) {
			//set dynamic extra fields
			if(m.isNotPathwayData()) {
				m.setUploaded((new File(m.getDataArchiveName()).exists()));
			} else {		
				Long accessed = logEntitiesRepository.downloads(m.standardName());
				m.setNumAccessed(accessed);
				m.setUploaded((new File(m.getDataArchiveName()).exists()));
				m.setPremerged(!m.getContent().isEmpty());
			}
		}
		
    	return list;
    }
    
    @RequestMapping("/metadata/datasources/{identifier}")
    public  @ResponseBody Metadata datasource(@PathVariable String identifier) {
		Metadata m = metadataDAO.getMetadataByIdentifier(identifier);
		if(m==null)
			return null;
		
		//set dynamic extra fields
		if(m.isNotPathwayData()) {
			m.setUploaded((new File(m.getDataArchiveName()).exists()));
		} else {		
			Long accessed = logEntitiesRepository.downloads(m.standardName());
			m.setNumAccessed(accessed);
			m.setUploaded((new File(m.getDataArchiveName()).exists()));
			m.setPremerged(!m.getContent().isEmpty());
		}
		
    	return m;
    }    
    
    
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
    	
    	Metadata existing = metadataDAO.getMetadataByIdentifier(metadata.identifier);
    	if(existing == null) {
    		metadataDAO.saveMetadata(metadata);
    	} else {
    		existing.setAvailability(metadata.getAvailability());
    		existing.setCleanerClassname(metadata.getCleanerClassname());
    		existing.setConverterClassname(metadata.getConverterClassname());
    		existing.setDescription(metadata.getDescription());
    		existing.setIconUrl(metadata.getIconUrl());
    		existing.setName(metadata.getName());
    		existing.setPubmedId(metadata.getPubmedId());
    		existing.setType(metadata.getType());
    		existing.setUrlToData(metadata.getUrlToData());
    		existing.setUrlToHomepage(metadata.getUrlToHomepage());   		
    		metadataDAO.saveMetadata(existing);
    	}
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
    	
    	Metadata existing = metadataDAO.getMetadataByIdentifier(metadata.identifier);
    	if(existing == null) {
    		metadataDAO.saveMetadata(metadata);
    	} else {
    		response.sendError(Status.BAD_REQUEST.getErrorCode(), 
                "PUT failed: Metadata already exists for pk: " + metadata.identifier
                	+ " (use POST to update instead)");
    	}
    }
    
    @RequestMapping(value = "/admin/datasources/{identifier}", method = RequestMethod.DELETE)
    public void delete(@PathVariable String identifier, HttpServletResponse response) throws IOException 
    {	
    	Metadata existing = metadataDAO.getMetadataByIdentifier(identifier);
    	if(existing != null) {
    		metadataDAO.deleteMetadata(existing);//clear files
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
    	Metadata m = metadataDAO.getMetadataByIdentifier(identifier);
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
    		Set<String> im = mappingsRepository.map(i);
    		if(im == null) {
    			res.put(i, null);
    		} else {
    			for(String ac : im)
    				res.put(i, ac);
    		}			
    	}		

    	//log to db (for usage reports)
    	LogUtils.log(logEntitiesRepository, 
    			events, Geoloc.fromIpAddress(clientIpAddress(request)));

    	return res;
	}
 
    
    private List<ValInfo> validationInfo() {
    	final List<ValInfo> list = new ArrayList<ValInfo>();
    	
		for(Metadata m : metadataDAO.getAllMetadata()) {
			if(m.isNotPathwayData())
				continue;
			
			ValInfo vi = new ValInfo();
			vi.setIdentifier(m.getIdentifier());
			
			for(Content pd : m.getContent())
				vi.getFiles().put(pd.getFilename(), pd + "; " + status(pd));
			
			list.add(vi);
		}
    		
		return list;
	}
    
    
    private String status(Content pd) {
    	if(pd.getValid() == null)
    		return "not validated or skipped";
    	else if(pd.getValid())
    		return "no critical errors";
    	else 
    		return "has critical errors";
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