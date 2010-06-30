package cpath.converter;

// imports
import org.biopax.paxtools.model.Model;

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
	 * @param model Model
	 */
	void convert(final InputStream is, final Model model);
}
