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
	private Class<E> applyTo;
	
	public SearchFilterAdapter(Class<E> applyTo) {
		values = new HashSet<T>();
		this.applyTo = applyTo;
	}
	
	@Override
	public void setValues(T[] values) {
		this.values.clear();
		if(values != null)
			Collections.addAll(this.values, values);
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() 
			+ " apply to:" + applyTo.getSimpleName()
			+ ";  filter values: " 
			+ values.toString();
	}

}
