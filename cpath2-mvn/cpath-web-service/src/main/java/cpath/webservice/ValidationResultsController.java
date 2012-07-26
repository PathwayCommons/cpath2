package cpath.webservice;

import cpath.service.CPathService;
import cpath.service.jaxb.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.validator.result.ValidatorResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


/**
 * 
 * @author rodche
 */
@Controller
public class ValidationResultsController extends BasicController 
{
    
	private static final Log log = LogFactory.getLog(ValidationResultsController.class);    
	
    private CPathService service; // main PC db access
	
    public ValidationResultsController(CPathService service) {
		this.service = service;
	}

    
    //TODO refactor to add one more RESTful level: /validation/{metadataId}/, /validation/{metadataId}/{pathway_pk}

    @RequestMapping("/validation/{metadataId}")
    public @ResponseBody ValidatorResponse queryForValidation(@PathVariable String metadataId) 
    {
    	if (log.isInfoEnabled())
			log.info("Query for validation object: " + metadataId);
    	
    	ServiceResponse result = service.getValidationReport(metadataId);
    	// TODO this may throw an exception, which we can make a ErrorResponse instead... later
    	ValidatorResponse body = (ValidatorResponse) ((DataResponse) result).getData();
    	
    	return body;
    }

    
    @RequestMapping("/validation/{metadataId}.html")
    public String queryForValidation(@PathVariable String metadataId, Model model) 
    {
    	if (log.isInfoEnabled())
			log.info("Query for validation html:" + metadataId);

    	ServiceResponse result = service.getValidationReport(metadataId);
    	// TODO this may throw an exception, which we can make a ErrorResponse instead... later
    	ValidatorResponse body = (ValidatorResponse) ((DataResponse) result).getData();
		model.addAttribute("response", body);
		
		return "validationSummary";
    }
	
}