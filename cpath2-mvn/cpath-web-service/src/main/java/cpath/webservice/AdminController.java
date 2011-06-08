package cpath.webservice;

import java.util.Map;

import cpath.service.CPathService;
import cpath.service.CPathService.ResultMapKey;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.validator.result.ValidatorResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;


/**
 * 
 * @author rodche
 */
@Controller
public class AdminController extends BasicController 
{
    
	private static final Log log = LogFactory.getLog(AdminController.class);    
	
	
    public AdminController(CPathService service) {
		this.service = service;
	}
    

	@InitBinder
    public void initBinder(WebDataBinder binder) {
		super.initBinder(binder);
    }

	
	// Get by ID (URI) command
    @RequestMapping("/validation/{metadataId}")
    public @ResponseBody ValidatorResponse queryForValidation(@PathVariable String metadataId) 
    {
    	if (log.isInfoEnabled())
			log.info("Query for validation object: " + metadataId);

    	Map<ResultMapKey, Object> result = service.getValidationReport(metadataId);
    	ValidatorResponse body = (ValidatorResponse) getBody(result, null, metadataId, ResultMapKey.ELEMENT);
		return body;
    }

    
    @RequestMapping("/validation/{metadataId}.html")
    public String queryForValidation(@PathVariable String metadataId, Model model) 
    {
    	if (log.isInfoEnabled())
			log.info("Query for validation html:" + metadataId);

    	Map<ResultMapKey, Object> result = service.getValidationReport(metadataId);
    	ValidatorResponse body = (ValidatorResponse) getBody(result, null, metadataId, ResultMapKey.ELEMENT);
		model.addAttribute("response", body);
		return "validationSummary";
    }
	
}