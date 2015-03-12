package cpath.service;

import java.util.Set;

import org.biopax.paxtools.controller.PathAccessor;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.util.IllegalBioPAXArgumentException;

import cpath.service.jaxb.TraverseEntry;
import cpath.service.jaxb.TraverseResponse;

/**
 * 
 * 
 * @author rodche
 *
 */
final class TraverseAnalysis implements Analysis<Model> {
	
	private final TraverseResponse callback;
	private final String[] uris;
	
	public TraverseAnalysis(TraverseResponse callback, String... uris) {
		this.callback = callback;
		this.uris = uris;
	}

	/* (non-Javadoc)
	 * @see cpath.dao.Analysis#execute(org.biopax.paxtools.model.Model)
	 */
	@Override
	public void execute(Model model) {
		
		final String propertyPath = callback.getPropertyPath();
		callback.getTraverseEntry().clear();
		
		PathAccessor pathAccessor = null; 
		try {
			pathAccessor = new PathAccessor(propertyPath, model.getLevel());
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to parse " +
				"the BioPAX property path: " + propertyPath, e);
		}
		
		for(String uri : uris) {
			BioPAXElement bpe = model.getByID(uri);
			try {
				Set<?> v = pathAccessor.getValueFromBean(bpe);
				TraverseEntry entry = new TraverseEntry();
				entry.setUri(uri);
				if(!pathAccessor.isUnknown(v)) {
//					entry.getValue().addAll(v);
					for(Object o : v) {
						if(o instanceof BioPAXElement) 
							entry.getValue().add(((BioPAXElement) o).getRDFId());
						else
							entry.getValue().add(String.valueOf(o));
					}
				}
				// add (it might have no values, but the path is correct)
				callback.getTraverseEntry().add(entry); 
			} catch (IllegalBioPAXArgumentException e) {
				// log, ignore if the path does not apply
			}
		}
	}

}
