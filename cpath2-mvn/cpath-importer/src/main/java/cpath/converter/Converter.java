package cpath.converter;

// imports

import java.io.InputStream;

/**
 * Converters convert molecules annotation into
 * biopax objects.
 */
public interface Converter {
	
	/**
	 * Converts the given protein or chemical annotation data
	 * into a paxtools model which contains EntityReferences only.
	 * Model is set/passed by reference.
	 *
	 * @param is InputStream
	 */
	void convert(final InputStream is);
}
