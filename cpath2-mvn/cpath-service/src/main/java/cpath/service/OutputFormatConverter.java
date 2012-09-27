package cpath.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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

    
    /**
     * Converts a BioPAX model to the cPath2 Extended Binary SIF format
     * using two output streams, - edges and nodes.
     * 
     * @param model
     * @param edgeStream
     * @param nodeStream
     * @throws IOException
     */
    void convertToExtendedBinarySIF(Model model, OutputStream edgeStream, OutputStream nodeStream) throws IOException;
}