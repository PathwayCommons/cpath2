/**
 ** Copyright (c) 2010 Memorial Sloan-Kettering Cancer Center (MSKCC)
 ** and University of Toronto (UofT).
 **
 ** This is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** any later version.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** both UofT and MSKCC have no obligations to provide maintenance, 
 ** support, updates, enhancements or modifications.  In no event shall
 ** UofT or MSKCC be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** UofT or MSKCC have been advised of the possibility of such damage.  
 ** See the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this software; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA;
 ** or find it at http://www.fsf.org/ or http://www.gnu.org.
 **/

package cpath.webservice;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

import cpath.config.CPathSettings;
import cpath.config.CPathSettings.CPath2Property;
import cpath.service.CPathService;
import cpath.service.GraphType;
import cpath.service.OutputFormat;
import cpath.service.jaxb.*;
import cpath.webservice.args.Get;
import cpath.webservice.args.GetProperty;
import cpath.webservice.args.binding.BiopaxTypeEditor;
import cpath.webservice.args.binding.OutputFormatEditor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.model.level3.Protein;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

/**
 * cPathSquared Model Access Web Service.
 * 
 * @author rodche
 */
@Controller
public class BiopaxModelController extends BasicController {
    private static final Log log = LogFactory.getLog(BiopaxModelController.class);    
	
    private static final String xmlBase = CPathSettings.get(CPath2Property.XML_BASE);
    
    private CPathService service; // main PC db access
	
    public BiopaxModelController(CPathService service) {
		this.service = service;
	}
    
    /**
	 * This configures the web request parameters binding, i.e., 
	 * conversion to the corresponding java types; for example,
	 * "neighborhood" is recognized as {@link GraphType#NEIGHBORHOOD},  
	 *  "protein" - {@link Protein} , etc.
	 *  Depending on the editor, illegal query parameters may result 
	 *  in an error or just NULL value.
	 * 
	 * @param binder
	 */
	@InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(OutputFormat.class, new OutputFormatEditor());
        binder.registerCustomEditor(Class.class, new BiopaxTypeEditor());
    }
	
	
	
	/**
	 * This convenience method is to make cpath2 data 
	 * more suitable for LinkedData / Semantic Web, by 
	 * resolving a cpath2-generated URI (such URIs are
	 * created in the data warehouse and during validation
	 * and normalization).
	 * 
	 * @param localId - e.g., cpath2 Metadata identifier (datasource) 
	 * or generated utility class local ID (32-byte hex string)
	 */
	@RequestMapping("/{localId}")
	public void cpathIdInfo(@PathVariable String localId, 
			Writer writer, HttpServletResponse response) throws IOException {
			Get get = new Get();
			get.setUri(new String[]{xmlBase + localId});
			elementById(get, null, writer, response);
	}
	
	
	// Get by ID (URI) command
    @RequestMapping("/get")
    public void elementById(@Valid Get get, BindingResult bindingResult, 
    	Writer writer, HttpServletResponse response) throws IOException
    {
    	if(bindingResult != null &&  bindingResult.hasErrors()) {
    		errorResponse(errorfromBindingResult(bindingResult), writer, response);
    	} else {
			OutputFormat format = get.getFormat();
			String[] uri = get.getUri();

			if (log.isInfoEnabled())
				log.info("Query: /get; format:" + format + ", urn:"
						+ Arrays.toString(uri));

			ServiceResponse result = service.fetch(format, uri);
			stringResponse(result, writer, response);
		}
    }  


	@RequestMapping("/top_pathways")
    public @ResponseBody SearchResponse topPathways()
    {
    	return service.topPathways();
    }
    
    
    @RequestMapping("/traverse")
    public @ResponseBody ServiceResponse traverse(@Valid GetProperty query, BindingResult bindingResult) 
    		throws IOException 
    {
    	if(bindingResult.hasErrors()) {
    		return errorfromBindingResult(bindingResult);
    	} else {
			return service.traverse(query.getPath(), query.getUri());
    	}
    }
    
}