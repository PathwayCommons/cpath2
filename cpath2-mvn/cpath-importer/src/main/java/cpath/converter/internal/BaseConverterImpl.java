package cpath.converter.internal;

// imports
import cpath.importer.Converter;

import org.biopax.paxtools.model.Model;

/**
 * General implementation of Converter interface.
 */
abstract class BaseConverterImpl implements Converter {
	
	protected Model model;
		
	@Override
	public void setModel(Model model) {
		this.model = model;
	}
		
}
