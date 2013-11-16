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

import org.biopax.paxtools.query.algorithm.Direction;
import org.biopax.paxtools.query.algorithm.LimitType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

/**
 * cPathSquared Main Web Service.
 * 
 * @author rodche
 */
@Controller
public class GraphController extends BasicController {
	private static final Logger log = LoggerFactory.getLogger(GraphController.class);    
	
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

	
	@RequestMapping("/graph")
	public void graphQuery(@Valid Graph graph, BindingResult bindingResult, 
			Writer writer, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
		log(request, 
			"kind="+graph.getKind(), "format="+graph.getFormat(), 
			"organisms="+Arrays.toString(graph.getOrganism()), 
			"datasource="+Arrays.toString(graph.getDatasource()),
			"direction="+graph.getDirection(), "limit="+graph.getLimit()
			);
		//TODO add dsNames.toString() if available; add status code
		
		//check for binding errors
		if(bindingResult.hasErrors()) {
			errorResponse(Status.BAD_REQUEST, 
					errorFromBindingResult(bindingResult), response);
			return;
		} 
		
		ServiceResponse result;
		
		switch (graph.getKind()) {
		case NEIGHBORHOOD:
			result = service.getNeighborhood(graph.getFormat(), graph.getSource(), 
				graph.getLimit(), graph.getDirection(), graph.getOrganism(), graph.getDatasource());
			break;
		case PATHSBETWEEN:
			result = service.getPathsBetween(graph.getFormat(), graph.getSource(), 
				graph.getLimit(), graph.getOrganism(), graph.getDatasource());
			break;
		case PATHSFROMTO:
			result = service.getPathsFromTo(graph.getFormat(), graph.getSource(), 
				graph.getTarget(), graph.getLimit(), graph.getOrganism(), graph.getDatasource());
			break;
		case COMMONSTREAM:
			result = service.getCommonStream(graph.getFormat(), graph.getSource(), 
				graph.getLimit(), graph.getDirection(), graph.getOrganism(), graph.getDatasource());
			break;
		default:
			// impossible (should has failed earlier)
			errorResponse(Status.INTERNAL_ERROR, 
				getClass().getCanonicalName() + " does not support " 
					+ graph.getKind(), response);
			
			return;
		}
		
		stringResponse(result, writer, response);
    }

}