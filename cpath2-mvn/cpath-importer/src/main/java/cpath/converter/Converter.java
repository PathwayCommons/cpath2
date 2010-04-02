package cpath.converter;

// imports
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.BioPAXLevel;

import java.io.InputStream;

/**
 * Converters convert protein annotation into
 * biopax objects.
 */
public interface Converter {

	/**
	 * Converts the given protein annotation data
	 * into a paxtools model which contains EntityReferences only.
	 * Model is set/passed by reference.  The biopax level of model
	 * is determined by level parameter.
	 *
	 * @param is InputStream
	 * @praam level BioPAXLevel
	 * @return Model
	 */
	void convert(InputStream is, BioPAXLevel level, Model model);
}
