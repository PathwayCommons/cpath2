package cpath.service.api;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Converter interface,  
 * to convert other format data to BioPAX.
 */
public interface Converter {
	
	/**
	 * Sets the xml:base (URI namespace) for
	 * the resulting BioPAX model.
	 * @param xmlBase
	 */
	void setXmlBase(String xmlBase);
	
	/**
	 * Builds and saves a new BioPAX model from the input data.
	 * @param is
	 * @param os  - result; the stream must be closed inside this method.
	 */
	void convert(InputStream is, OutputStream os);
}
