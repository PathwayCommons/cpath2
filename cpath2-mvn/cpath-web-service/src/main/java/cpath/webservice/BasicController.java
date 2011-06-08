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

import java.util.Arrays;
import java.util.Map;

import cpath.service.CPathService;
import cpath.service.CPathService.OutputFormat;
import cpath.service.CPathService.ResultMapKey;
import cpath.service.internal.ProtocolStatusCode;
import cpath.service.jaxb.ErrorType;
import cpath.webservice.args.*;
import cpath.webservice.args.binding.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Protein;
import org.biopax.paxtools.query.algorithm.Direction;
import org.biopax.paxtools.query.algorithm.LimitType;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;

/**
 * Basic (abstract) controller.
 * 
 * @author rodche
 */
public abstract class BasicController {
    private static final Log log = LogFactory.getLog(BasicController.class);
    protected static String newline = System.getProperty("line.separator");
    
    @NotNull
    protected CPathService service; // main PC db access
    
	
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
    protected void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(OutputFormat.class, new OutputFormatEditor());
        binder.registerCustomEditor(GraphType.class, new GraphTypeEditor());
        binder.registerCustomEditor(Direction.class, new GraphQueryDirectionEditor());
        binder.registerCustomEditor(LimitType.class, new GraphQueryLimitEditor());
        binder.registerCustomEditor(Class.class, new BiopaxTypeEditor());
        binder.registerCustomEditor(Cmd.class, new CmdEditor());
        binder.registerCustomEditor(CmdArgs.class, new CmdArgsEditor());
        binder.registerCustomEditor(OrganismDataSource.class, new OrganismDataSourceEditor());
        binder.registerCustomEditor(PathwayDataSource.class, new PathwayDataSourceEditor());
    }
 
	
	protected ErrorType errorfromBindingResult(BindingResult bindingResult) {
		ErrorType error = ProtocolStatusCode.BAD_REQUEST.createErrorType();
		StringBuffer sb = new StringBuffer();
		for (FieldError fe : bindingResult.getFieldErrors()) {
			sb.append(fe.getField() + " was " + fe.getRejectedValue() + "; "
					+ fe.getDefaultMessage() + ". ");
		}
		error.setErrorDetails(sb.toString());
		return error;
	}
    
    
    protected ErrorType errorFromResults(Object err, ProtocolStatusCode statusCode) 
    {
    	ErrorType error = statusCode.createErrorType();
		if(err instanceof Exception) {
			error.setErrorDetails(err.toString() + "; " 
				+ Arrays.toString(((Exception)err).getStackTrace()));
		} else {
			error.setErrorDetails(err.toString());
		}
		return error;
    }

    
    protected Object getBody(Map<ResultMapKey, Object> results, OutputFormat format, String details, ResultMapKey mapKey) {
    	Object toReturn = null;
    	
		if (!results.containsKey(ResultMapKey.ERROR)) {
			toReturn = results.get(mapKey);
			
			if(toReturn == null || 
				(mapKey == ResultMapKey.DATA && "".equals(toReturn.toString().trim()))
			){
				toReturn = ProtocolStatusCode.errorAsXml(ProtocolStatusCode.NO_RESULTS_FOUND, 
						"Empty result for: " + details);
			} else if(OutputFormat.BIOPAX == format) {
				Model m = (Model) results.get(ResultMapKey.MODEL);
				if(m == null || m.getObjects().isEmpty()) {
					toReturn = ProtocolStatusCode.errorAsXml(ProtocolStatusCode.NO_RESULTS_FOUND, 
							"Empty result for: " + details);
				}
			}
		} else {
			toReturn = ProtocolStatusCode.marshal(
					errorFromResults(results.get(ResultMapKey.ERROR), ProtocolStatusCode.INTERNAL_ERROR)		
			);
		}
		
		return toReturn;
	}    
}