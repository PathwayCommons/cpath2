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
import java.util.Arrays;
import java.util.Map;

import cpath.service.CPathService;
import cpath.service.Cmd;
import cpath.service.GraphType;
import cpath.service.ProtocolStatusCode;
import cpath.service.CPathService.ResultMapKey;
import cpath.service.OutputFormat;
import cpath.service.jaxb.ErrorType;
import cpath.webservice.args.*;
import cpath.webservice.args.binding.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.model.level3.Protein;
import org.biopax.paxtools.query.algorithm.Direction;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * cPathSquared Main Web Service.
 * 
 * @author rodche
 */
@Controller
public class GraphController extends BasicController {
    private static final Log log = LogFactory.getLog(GraphController.class);    
	
	
    public GraphController(CPathService service) {
		this.service = service;
	}
    
    /**
	 * This configures the web request parameters binding, i.e., 
	 * conversion to the corresponding java types; for example,
	 * "neighborhood" is recognized as {@link GraphType#NEIGHBORHOOD}, 
	 * "search" will become {@link Cmd#SEARCH}, 
	 *  "protein" - {@link Protein} , etc.
	 *  Depending on the editor, illegal query parameters may result 
	 *  in an error or just NULL value (see e.g., {@link CmdArgsEditor})
	 * 
	 * @param binder
	 */
	@InitBinder
    public void initBinder(WebDataBinder binder) {
        super.initBinder(binder);
        /* when GraphQueryValidator is set here,
         * it prevents javax.validation.Validator instance (JSR-303)
         * from checking first; so - we will use it inside controller methods
         //  binder.setValidator(new GraphQueryValidator());
         */
    }

	@RequestMapping("/graph")
//    public @ResponseBody String graphQuery(@Valid Graph graph, BindingResult bindingResult)
	public void graphQuery(@Valid Graph graph, BindingResult bindingResult, Writer writer) throws IOException
    {
		//check for binding errors
		if(bindingResult.hasErrors()) {
			ErrorType error = errorfromBindingResult(bindingResult);
//			return ProtocolStatusCode.marshal(error);
			writer.write(ProtocolStatusCode.marshal(error));
			return;
		} 
		
    	// additional validation of query parameters
		DataBinder binder = new DataBinder(graph);
		binder.setValidator(new GraphQueryValidator());
		binder.validate();
		bindingResult = binder.getBindingResult();
		if(bindingResult.hasErrors()) {
			ErrorType error = errorfromBindingResult(bindingResult);
//			return ProtocolStatusCode.marshal(error);
			writer.write(ProtocolStatusCode.marshal(error));
			return;
		}
		
		Object response = null;
		
		OutputFormat format = graph.getFormat();
		GraphType kind = graph.getKind();
		String[] source = graph.getSource();
		String[] target = graph.getTarget();
		Integer limit = graph.getLimit();
		Direction direction = graph.getDirection();
		String sources = Arrays.toString(source);
		String targets = Arrays.toString(target);
		
		if(log.isInfoEnabled()) {
			log.info("GraphQuery format:" + format + ", kind:" + kind
				+ ", source:" + sources + ", target:" + targets
				+ ", limit: " + limit + ", direction: " + direction
			);
		}
		
		Map<ResultMapKey, Object> result;
		
		switch (kind) {
		case NEIGHBORHOOD:
			result = service.getNeighborhood(format, source, limit, direction);
			response = parseResultMap(result, format, "nearest neighbors of " 
				+ sources, ResultMapKey.DATA);
			break;
		case PATHSBETWEEN:
			result = service.getPathsBetween(format, source, target, limit);
			response = parseResultMap(result, format, "paths between sources: " + sources
				+ " and targets: " + targets, ResultMapKey.DATA);
			break;
		case COMMONSTREAM:
			result = service.getCommonStream(format, source, limit, direction);
			response = parseResultMap(result, format, "common " + direction + "stream of " +
					sources, ResultMapKey.DATA);
			break;
		default:
			// impossible (should has failed earlier)
			break;
		}

//		return (response instanceof ErrorType)
//			? ProtocolStatusCode.marshal((ErrorType) response)
//				: (String)response;
		
		writer.write(
			(response instanceof ErrorType)
			? ProtocolStatusCode.marshal((ErrorType) response)
				: (String)response
		);
    }

}