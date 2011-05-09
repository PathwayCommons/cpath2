package cpath.service.analyses;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;

import java.util.HashSet;
import java.util.Set;

/**
 * Common methods of different analyses go here.
 *
 * @author Ozgun Babur
 */
public class Common
{
	static Log log = LogFactory.getLog(Common.class);

	/**
	 * This method prepares the source and target sets of the queries.
	 *
	 * @param arg the argument corresponding to the specific source or target set of IDs
	 * @return related biopax elements
	 */
	protected static Set<BioPAXElement> getAllByID(Model model, Object arg)
	{
		// Source elements
		Set<BioPAXElement> elements = new HashSet<BioPAXElement>();

		// IDs of source elements
		Set<String> ids = (Set<String>) arg;

		// Fetch source objects using IDs

		for(Object id : ids)
		{
			BioPAXElement e = model.getByID(id.toString());

			if(e != null)
			{
				elements.add(e);
			}
			else
			{
				if (log.isWarnEnabled()) log.warn("Element not found: " + id);
			}
		}

		return elements;
	}
}