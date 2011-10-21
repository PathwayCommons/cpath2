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

import cpath.service.CPathService;
import cpath.service.GraphType;
import cpath.service.OutputFormat;
import cpath.service.Status;
import cpath.service.jaxb.*;
import cpath.webservice.args.*;
import cpath.webservice.args.binding.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.query.algorithm.Direction;
import org.biopax.paxtools.query.algorithm.LimitType;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * cPathSquared Main Web Service.
 * 
 * @author rodche
 */
@Controller
public class GraphController extends BasicController {
    private static final Log log = LogFactory.getLog(GraphController.class);    
	
    @NotNull
    private CPathService service; // main PC db access
	
    public GraphController(CPathService service) {
		this.service = service;
	}
    
    /**
	 * This configures the web request parameters binding, i.e., 
	 * conversion to the corresponding java types; for example,
	 * "neighborhood" is recognized as {@link GraphType#NEIGHBORHOOD}.
	 *  Depending on the editor, illegal query parameters may result 
	 *  in an error or just NULL value.
	 * 
	 * @param binder
	 */
	@InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(OutputFormat.class, new OutputFormatEditor());
        binder.registerCustomEditor(GraphType.class, new GraphTypeEditor());
        binder.registerCustomEditor(Direction.class, new GraphQueryDirectionEditor());
        binder.registerCustomEditor(LimitType.class, new GraphQueryLimitEditor());
    }


//TODO re-factor as (same - for the rest of graph kinds)
//	@RequestMapping("/neighborhood")
//	public void neighborhood(@Valid NeighborhoodGraph graph, BindingResult bindingResult, Writer writer) throws IOException {
//		
//	}
	
	
	@RequestMapping("/graph")
	public void graphQuery(@Valid Graph graph, BindingResult bindingResult, Writer writer) throws IOException
    {
		//check for binding errors
		if(bindingResult.hasErrors()) {
			errorResponse(errorfromBindingResult(bindingResult), writer);;
			return;
		} 
		
		log("/graph", graph);
		
		ServiceResponse result = new ServiceResponse();
		
		switch (graph.getKind()) {
		case NEIGHBORHOOD:
			result = service.getNeighborhood(graph.getFormat(), graph.getSource(), graph.getLimit(), graph.getDirection());
			break;
		case PATHSBETWEEN:
			result = service.getPathsBetween(graph.getFormat(), graph.getSource(), graph.getLimit());
			break;
		case PATHSOFINTEREST:
			result = service.getPathsOfInterest(graph.getFormat(), graph.getSource(), graph.getTarget(), graph.getLimit());
			break;
		case COMMONSTREAM:
			result = service.getCommonStream(graph.getFormat(), graph.getSource(), graph.getLimit(), graph.getDirection());
			break;
		default:
			// impossible (should has failed earlier)
			result.setResponse(Status.INTERNAL_ERROR
				.errorResponse(getClass().getCanonicalName() + 
					" does not support " + graph.getKind()));
			break;
		}
		
		stringResponse(result, writer);
    }

	
	private void log(String method, Graph graph) {
		if(log.isInfoEnabled()) {
			log.info(method + " query; format:" + graph.getFormat() 
				+ ", kind:" + graph.getKind()
				+ ", source:" + Arrays.toString(graph.getSource())
				+ ", target:" + Arrays.toString(graph.getTarget())
				+ ", limit: " + graph.getLimit() 
				+ ", direction: " + graph.getDirection()
			);
		}
	}

}