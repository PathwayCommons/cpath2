package cpath.converter.internal;

// imports
import cpath.dao.PaxtoolsDAO;
import cpath.importer.Converter;

import java.io.InputStream;

import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;

/**
 * General implementation of Converter interface.
 */
class BaseConverterImpl implements Converter {
	
	protected Model model;
	protected static final BioPAXFactory factory = 
		BioPAXLevel.L3.getDefaultFactory();
		
	@Override
	public void setModel(Model model) {
		this.model = model;
	}
	
	/**
	 * (non-Javadoc>
	 * @see cpath.importer.Converter#convert(java.io.InputStream)
	 */
	@Override
	public void convert(final InputStream is) {}
	
	protected <T extends BioPAXElement> T getById(String urn, Class<T> type) {
		T bpe = (T) model.getByID(urn);
		if (bpe != null && model instanceof PaxtoolsDAO) {
			// initialize before finally detaching it
			((PaxtoolsDAO) model).initialize(bpe);
		}
		return bpe;
	}
	
}
