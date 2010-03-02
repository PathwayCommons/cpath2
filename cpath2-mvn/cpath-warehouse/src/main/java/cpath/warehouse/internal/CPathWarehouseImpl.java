package cpath.warehouse.internal;

// imports
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.level3.UtilityClass;
import org.springframework.stereotype.Service;

import cpath.warehouse.CPathWarehouse;
import cpath.warehouse.CvRepository;
import cpath.warehouse.metadata.MetadataDAO;

import java.util.Set;

/**
 * @author rodch
 *
 */
@Service
public final class CPathWarehouseImpl implements CPathWarehouse {

    private MetadataDAO metadataDAO;
    private CvRepository cvRepository;
	
	
	public CPathWarehouseImpl(MetadataDAO metadataDAO,
			CvRepository cvRepository) {
		this.metadataDAO = metadataDAO;
		this.cvRepository = cvRepository;
	}

	/* (non-Javadoc)
	 * @see cpath.warehouse.CPathWarehouse#createUtilityClass(java.lang.String, java.lang.Class)
	 */
	public <T extends UtilityClass> T createUtilityClass(String primaryUrn,
			Class<T> utilityClazz) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see cpath.warehouse.CPathWarehouse#createUtilityClass(java.lang.String)
	 */
	public <T extends UtilityClass> T createUtilityClass(String primaryUrn) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see cpath.warehouse.CPathWarehouse#createUtilityClass(java.lang.String, java.lang.Class, java.lang.String)
	 */
	public <T extends UtilityClass> T createUtilityClass(String primaryUrn,
			Class<? extends BioPAXElement> domain, String property) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see cpath.warehouse.CPathWarehouse#getAllChildrenOfCv(java.lang.String)
	 */
	public Set<String> getAllChildrenOfCv(String urn) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see cpath.warehouse.CPathWarehouse#getDirectChildrenOfCv(java.lang.String)
	 */
	public Set<String> getDirectChildrenOfCv(String urn) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see cpath.warehouse.CPathWarehouse#getParentsOfCv(java.lang.String)
	 */
	public Set<String> getParentsOfCv(String urn) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see cpath.warehouse.CPathWarehouse#getPrimaryURI(java.lang.String)
	 */
	public String getPrimaryURI(String urn) {
		// TODO Auto-generated method stub
		return null;
	}

}
