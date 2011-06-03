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
import org.biopax.paxtools.query.algorithm.LimitType;

import java.util.Set;

/**
 * Paths-between query. User provides a source set and a target set, the query returns paths from
 * source to target. If no target set is provided, the search is performed between the source set.
 *
 * @author ozgun
 *
 */
public class PathsBetweenAnalysis implements Analysis {

	static Log log = LogFactory.getLog(PathsBetweenAnalysis.class);
	
	/**
	 * Parameters to provide:
	 * source: IDs of source objects
	 * target: IDs of target objects
	 * limit: search distance limit
	 * limit-type: normal limit or shortest+k limit
	 */
	@Override
	public Set<BioPAXElement> execute(Model model, Object... args)
	{
		// Source elements
		Set<BioPAXElement> source = Common.getAllByID(model, args[0]);

		// Target elements
		Set<BioPAXElement> target;

		// If a different target set is provided, use it. Otherwise the search will be among the
		// source set.

		if (args[1] instanceof Set)
		{
			target = Common.getAllByID(model, args[1]);
		}
		else
		{
			target = source;
		}

		// Limit type
		LimitType limitType = (LimitType) args[3];

		// Search limit
		int limit = (Integer) args[2];

		// Execute the query
		return QueryExecuter.runPOI(source, target, model, limitType, limit);
	}

}