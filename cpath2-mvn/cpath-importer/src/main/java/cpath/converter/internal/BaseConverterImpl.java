package cpath.converter.internal;

// imports
import cpath.config.CPathSettings;
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

	// use for, e.g., xref's RDFId = L3_UNIFICATIONXREF_URI + URLEncoder.encode(db + "_" +  id);
	public static final String L3_UNIFICATIONXREF_URI = CPathSettings.CPATH_URI_PREFIX + "UnificationXref:";
	public static final String L3_PUBLICATIONXREF_URI = CPathSettings.CPATH_URI_PREFIX + "PublicationXref:";
	public static final String L3_RELATIONSHIPXREF_URI = CPathSettings.CPATH_URI_PREFIX + "RelationshipXref:";
	
	public static final String L2_UNIFICATIONXREF_URI = CPathSettings.CPATH_URI_PREFIX + "unificationXref:";
	public static final String L2_PUBLICATIONXREF_URI = CPathSettings.CPATH_URI_PREFIX + "publicationXref:";
	public static final String L2_RELATIONSHIPXREF_URI = CPathSettings.CPATH_URI_PREFIX + "relationshipXref:";
	
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
	
	/**
	 * (non-Javadoc>
	 * @see cpath.converter.Converter#convert(java.io.InputStream)
	 */
	public void convert(final InputStream is) {}
	
	protected <T extends BioPAXElement> T getById(String urn, Class<T> type) 
    {
            /*return 
            (model instanceof WarehouseDAO) 
                    ? ((WarehouseDAO)model).getObject(urn, type) //completely detached
                            : (T) model.getByID(urn) ;      
            */
            T bpe = (T) model.getByID(urn);
            if(bpe != null && model instanceof PaxtoolsDAO) {
                    // initialize before finally detaching it
                    ((PaxtoolsDAO) model).initialize(bpe);
            }
            return bpe;
    }
}
