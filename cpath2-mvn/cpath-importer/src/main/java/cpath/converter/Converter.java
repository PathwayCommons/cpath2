package cpath.converter;

// imports

import java.io.InputStream;

/**
 * Converters convert protein annotation into
 * biopax objects.
 */
public interface Converter {
	
	/**
	 * Converts the given protein annotation data
	 * into a paxtools model which contains EntityReferences only.
	 * Model is set/passed by reference.
	 *
	 * @param is InputStream
	 */
	void convert(final InputStream is);
}
