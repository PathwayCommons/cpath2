package cpath.service;

import java.io.InputStream;

import org.biopax.paxtools.model.Model;

import cpath.service.jaxb.ErrorResponse;
import cpath.service.jaxb.ServiceResponse;

public interface OutputFormatConverter {

	/**
	 * For the given BioPAX data stream, converts it to the 
	 * desired output format.
	 *
	 * @param biopax
	 * @param format
	 *  
	 * @return
	 */
	ServiceResponse convert(InputStream biopax, OutputFormat format);
	
	
	/**
     * Converts the BioPAX data into the other format.
     * 
     * @param model
     * @param format
     * 
     * @return data response with the converted data or {@link ErrorResponse}.
     */
    ServiceResponse convert(Model model, OutputFormat format);

}