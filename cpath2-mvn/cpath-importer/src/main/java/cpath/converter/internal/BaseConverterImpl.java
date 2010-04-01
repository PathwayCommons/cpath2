package cpath.converter.internal;

// imports
import cpath.converter.Converter;

import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.BioPAXLevel;

import java.io.InputStream;

/**
 * General implementation of Converter interface.
 */
public class BaseConverterImpl implements Converter {

	/**
	 * (non-Javadoc>
	 * @see cpath.converter.Converter#convert(java.io.InputStream, org.biopax.paxtools.model.BioPXLevel, org.biopax.paxtools.model.Model)
	 */
	public void convert(final InputStream is, BioPAXLevel level, Model model) {}
}
