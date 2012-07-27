package cpath.webservice;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import cpath.service.CPathService;
import cpath.service.jaxb.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.validator.result.ValidatorResponse;
import org.biopax.validator.utils.BiopaxValidatorUtils;
import org.springframework.core.io.DefaultResourceLoader;
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
    
    @RequestMapping("/validation/{metadataId}/files.html") //a JSP view
    public String queryForValidations(@PathVariable String metadataId, Model model) 
    {
    	if (log.isInfoEnabled())
			log.info("Query - all validations (separate files) by datasource: " + metadataId);
    	
    	ServiceResponse result = service.getPathwayDataInfo(metadataId);
    	
    	model.addAttribute("identifier", metadataId);
    	if(result instanceof DataResponse)
    		model.addAttribute("results", ((Map)((DataResponse)result).getData()).entrySet());
    	
    	return "validations";
    }
    
    
    @RequestMapping("/validation/file/{pk}.html") // now view; the HTML is written to the response stream
    public void queryForValidationByPkHtml(@PathVariable Integer pk, Writer writer) throws IOException 
    {	
    	ValidatorResponse resp = queryForValidationByPk(pk);
    	//the xslt stylesheet exists in the biopax-validator-core module
		Source xsl = new StreamSource((new DefaultResourceLoader())
			.getResource("classpath:html-result.xsl").getInputStream());
		BiopaxValidatorUtils.write(resp, writer, xsl); 
    }
  
    
    @RequestMapping("/validation/file/{pk}") //XML report
    public @ResponseBody ValidatorResponse queryForValidationByPk(@PathVariable Integer pk) 
    {
    	ServiceResponse result = service.getValidationReport(pk);
    	// TODO this may throw an exception, which we can make a ErrorResponse instead... later
    	ValidatorResponse body = (ValidatorResponse) ((DataResponse) result).getData();
    	
    	return body; //XML output (marshaled automatically)
    }

    
    @RequestMapping("/validation/{metadataId}")
    public @ResponseBody ValidatorResponse queryForValidation(@PathVariable String metadataId) 
    {
    	if (log.isInfoEnabled())
			log.info("Query - all validation reports for: " + metadataId);
    	
    	ServiceResponse result = service.getValidationReport(metadataId);
    	// TODO this may throw an exception, which we can make a ErrorResponse instead... later
    	ValidatorResponse body = (ValidatorResponse) ((DataResponse) result).getData();
    	
    	return body;
    }

    
    @RequestMapping("/validation/{metadataId}.html") //a JSP view
    public String queryForValidation(@PathVariable String metadataId, Model model) 
    {
    	if (log.isInfoEnabled())
			log.info("Query - all validation reports, as html, for:" + metadataId);

    	ServiceResponse result = service.getValidationReport(metadataId);
    	// TODO this may throw an exception, which we can make a ErrorResponse instead... later
    	ValidatorResponse body = (ValidatorResponse) ((DataResponse) result).getData();
		model.addAttribute("response", body);
		
		return "validationSummary";
    }
	
}