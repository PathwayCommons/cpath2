package cpath.converter.internal;

// imports
import cpath.converter.Converter;
import cpath.dao.PaxtoolsDAO;

import java.io.InputStream;

import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;

/**
 * General implementation of Converter interface.
 */
public class BaseConverterImpl implements Converter {
	
	protected Model model;
	protected static final BioPAXFactory factory = 
		BioPAXLevel.L3.getDefaultFactory();
	
	public BaseConverterImpl() {
	}
	
	public BaseConverterImpl(Model model) {
		this.model = model;
	}
	
	public void setModel(Model model) {
		this.model = model;
	}
	
	public Model getModel() { return model; }
	
	/**
	 * (non-Javadoc>
	 * @see cpath.converter.Converter#convert(java.io.InputStream)
	 */
	public void convert(final InputStream is) {}
	
	protected <T extends BioPAXElement> T getById(String urn, Class<T> type) {
		T bpe = (T) model.getByID(urn);
		if (bpe != null && model instanceof PaxtoolsDAO) {
			// initialize before finally detaching it
			((PaxtoolsDAO) model).initialize(bpe);
		}
		return bpe;
	}
	
    
	/**
	 * For the given converter class name,
	 * returns an instance of a class which
	 * implements the converter interface.
	 *
	 * @param converterClassName String
	 * @return Converter
	 */
	public static Converter getConverter(final String converterClassName) {
		try {
			Class<?> converterClass = Class.forName(converterClassName);
			return (Converter) converterClass.newInstance();
		}
		catch (Exception e) {
//			log.error("could not create converter class " 
//					+ converterClassName, e);
		}
		return null;
	}
}
