package cpath.dao.internal.filters;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.biopax.paxtools.model.BioPAXElement;

import cpath.dao.filters.SearchFilter;

/**
 * @author rodche
 *
 * @param <E>
 * @param <T>
 */
public abstract class SearchFilterAdapter<E extends BioPAXElement, T> 
	implements SearchFilter<E, T> 
{
	protected Set<T> values;
	
	public SearchFilterAdapter() {
		values = new HashSet<T>();
	}
	
	@Override
	public void setValues(T[] values) {
		this.values.clear();
		Collections.addAll(this.values, values);
	}

}
