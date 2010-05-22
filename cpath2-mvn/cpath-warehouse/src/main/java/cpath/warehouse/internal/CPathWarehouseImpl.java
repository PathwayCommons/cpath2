package cpath.warehouse.internal;

// imports
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.model.level3.ControlledVocabulary;
import org.biopax.paxtools.model.level3.ProteinReference;
import org.biopax.paxtools.model.level3.SmallMoleculeReference;
import org.biopax.paxtools.model.level3.UtilityClass;
import org.springframework.stereotype.Service;

import cpath.dao.PaxtoolsDAO;
import cpath.warehouse.CPathWarehouse;
import cpath.warehouse.CvRepository;
import cpath.warehouse.MetadataDAO;

import java.util.Set;

/**
 * @author rodch
 *
 */
@Service
public final class CPathWarehouseImpl implements CPathWarehouse {
	private final static Log log = LogFactory.getLog(CPathWarehouseImpl.class);
	
    private CvRepository cvRepository;
    private PaxtoolsDAO moleculesDAO;
    private PaxtoolsDAO proteinsDAO;
    private MetadataDAO metadataDAO;
	
	
	public CPathWarehouseImpl(CvRepository cvRepository,
			PaxtoolsDAO moleculesDAO, MetadataDAO metadataDAO, PaxtoolsDAO proteinsDAO) {
		this.cvRepository = cvRepository;
		this.moleculesDAO = moleculesDAO;
		this.proteinsDAO = proteinsDAO;
		this.metadataDAO = metadataDAO;
	}


	
	public <T extends UtilityClass> T getObject(String primaryUrn,
			Class<T> utilityClazz) {
		if(SmallMoleculeReference.class.isAssignableFrom(utilityClazz)) {
			return (T) moleculesDAO.getElement(primaryUrn, true);
		} else if(ProteinReference.class.isAssignableFrom(utilityClazz)) {
			return (T) proteinsDAO.getElement(primaryUrn, true);
		} else if(ControlledVocabulary.class.isAssignableFrom(utilityClazz)) {
			// TODO create proper CV
		}
		
		return null;
	}


	public Set<String> getAllChildren(String urn) {
		// TODO Auto-generated method stub
		return null;
	}

	
	public Set<String> getDirectChildren(String urn) {
		// TODO Auto-generated method stub
		return null;
	}

	
	public Set<String> getAllParents(String urn) {
		// TODO Auto-generated method stub
		return null;
	}


	public Set<String> getDirectParents(String urn) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see cpath.warehouse.CPathWarehouse#getPrimaryURI(java.lang.String, java.lang.Class)
	 */
	@Override
	public String getPrimaryURI(String id,
			Class<? extends UtilityClass> utilityClass) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see cpath.warehouse.CvRepository#getControlledVocabulary(java.lang.String, java.lang.Class)
	 */
	@Override
	public <T extends ControlledVocabulary> T getControlledVocabulary(
			String urn, Class<T> cvClass) {
		return cvRepository.getControlledVocabulary(urn, cvClass);
	}



	/* (non-Javadoc)
	 * @see cpath.warehouse.CvRepository#getControlledVocabulary(java.lang.String, java.lang.String, java.lang.Class)
	 */
	@Override
	public <T extends ControlledVocabulary> T getControlledVocabulary(
			String db, String id, Class<T> cvClass) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see cpath.warehouse.CvRepository#isChild(java.lang.String, java.lang.String)
	 */
	@Override
	public boolean isChild(String parentUrn, String urn) {
		// TODO Auto-generated method stub
		return false;
	}



	/* (non-Javadoc)
	 * @see cpath.warehouse.CPathWarehouse#createIndex()
	 */
	@Override
	public void createIndex() {
		proteinsDAO.createIndex();
		moleculesDAO.createIndex();
	}
	
}
