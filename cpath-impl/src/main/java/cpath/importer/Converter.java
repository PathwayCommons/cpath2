package cpath.importer;

import java.io.InputStream;

import org.biopax.paxtools.model.Model;

/**
 * Converter interface. 
 * Is required for all classes to be used 
 * in order to convert other format to BioPAX L3
 * (to be saved in the cPath2 Warehouse).
 */
public interface Converter {
	
	void setInputStream(final InputStream is);
	
	void setXmlBase(String xmlBase);
	
	Model convert();
	
}
