package cpath.converter.internal;

// imports
import cpath.importer.Converter;

import java.io.InputStream;

import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;

/**
 * General implementation of Converter interface.
 */
class BaseConverterImpl implements Converter {
	
	protected Model model;
	protected static final BioPAXFactory factory = 
		BioPAXLevel.L3.getDefaultFactory();
		
	@Override
	public void setModel(Model model) {
		this.model = model;
	}
	
	/**
	 * (non-Javadoc>
	 * @see cpath.importer.Converter#convert(java.io.InputStream)
	 */
	@Override
	public void convert(final InputStream is) {}
		
}
