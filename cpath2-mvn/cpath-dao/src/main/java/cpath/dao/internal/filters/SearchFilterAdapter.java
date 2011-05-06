package cpath.dao.internal.filters;

import org.biopax.paxtools.model.BioPAXElement;

import cpath.dao.SearchFilter;

/**
 * @author rodche
 *
 * @param <E>
 * @param <T>
 */
public abstract class SearchFilterAdapter<E extends BioPAXElement, T> 
	implements SearchFilter<E, T> 
{
	protected T[] values;
	
	@Override
	public void setValues(T... values) {
		this.values = values;
	}

}
