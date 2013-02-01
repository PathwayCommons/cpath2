package cpath.importer;

// imports

import java.io.InputStream;

import org.biopax.paxtools.model.Model;

/**
 * Converters convert warehouse annotation into
 * biopax objects.
 */
public interface Converter {
	
	/**
	 * Sets the target BioPAX Model
	 * (to be created or modified as the result of data conversion)
	 * 
	 * @param model
	 */
	void setModel(Model model);
	
	/**
	 * Converts the given protein or chemical annotation data
	 * into a paxtools model which contains EntityReferences only.
	 * Model is set/passed by reference.
	 *
	 * @param is InputStream
	 * @param optionalArgs optional implementation-specific parameters (might be external resource locations, etc.)
	 */
	void convert(final InputStream is, Object... optionalArgs);
}
