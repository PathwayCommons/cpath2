/**
 ** Copyright (c) 2009 Memorial Sloan-Kettering Cancer Center (MSKCC)
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

package cpath.service.analyses;

import cpath.dao.Analysis;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.query.QueryExecuter;
import org.biopax.paxtools.query.algorithm.Direction;
import org.biopax.paxtools.query.wrapperL3.Filter;
import org.biopax.paxtools.query.wrapperL3.UbiqueFilter;

import java.util.List;
import java.util.Set;

/**
 * Neighborhood query using provided parameters.
 * source: IDs of source objects
 * limit: breadth of the neighborhood
 * upstream: true if neighborhood search will go upstream
 * downstream: true if neighborhood search will go downstream
 *
 * @author ozgun
 *
 */
public class NeighborhoodAnalysis implements Analysis {

	static Log log = LogFactory.getLog(NeighborhoodAnalysis.class);
	
	/**
	 * 
	 * @param model - a BioPAX {@link Model}
	 * @param args - are as follows: 
	 *   - first argument should be a String Set of source objects' IDs (URIs).
	 *   - second (Integer) is the neighborhood 'limit' (distance)
	 *   - third argument is is {@link Direction}
	 */
	@Override
	public Set<BioPAXElement> execute(Model model, Object... args)
	{
		// source elements
		Set<BioPAXElement> source = Common.getAllByID(model, args[0]);

		// search limit
		int limit = (Integer) args[1];

		// direction
		Direction direction = (Direction) args[2];

		// organism and datasource filters
		List<Filter> filters = Common.getOrganismAndDataSourceFilters(
			(String[]) args[3], (String[]) args[4]);

		// ubique filter
		filters.add(new UbiqueFilter((Set<String>) args[5]));

		// execute the query
		return QueryExecuter.runNeighborhood(source, model, limit, direction,
			filters.toArray(new Filter[filters.size()]));
	}

}