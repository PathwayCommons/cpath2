package cpath.cleaner.internal;

// imports
import java.lang.reflect.Method;

import cpath.importer.Cleaner;

import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.*;

/**
 * Implementation of Cleaner interface for use when data
 * does not need to be cleaned.
 */
class BaseCleanerImpl implements Cleaner {

	/**
	 * This basic method simply returns a copy of the original (string) 
	 * pathway data.
	 * Other, more specific cleaners, extending this class must override this method. 
	 * 
	 * @see cpath.importer.Cleaner#clean(PathwayData)
	 */
	@Override
	public String clean(final String pathwayData) {
		return new String(pathwayData);
	}
	

	/**
	 * Replaces the URI of a BioPAX object
	 * using java reflection. Normally, one should avoid this;
	 * please use when absolutely necessary, with care. 
	 * 
	 * @param model
	 * @param el
	 * @param newRDFId
	 */
	protected final void replaceID(Model model, Level3Element el, String newRDFId) {
		if(el.getRDFId().equals(newRDFId))
			return; // no action required
		
		model.remove(el);
		try {
			Method m = el.getClass().getDeclaredMethod("setRDFId", String.class);
			m.setAccessible(true);
			m.invoke(el, newRDFId);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		model.add(el);
	}
}
