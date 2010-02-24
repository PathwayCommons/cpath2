package cpath.warehouse.internal;

// imports
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.CPathWarehouse;
import cpath.warehouse.metadata.MetadataDAO;

import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.level3.UtilityClass;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * @author rodch
 *
 */
@Service
public final class CPathWarehouseImpl implements CPathWarehouse {

	/*
	 * TODO inject dependencies (e.g. several repositories)
	 */
    @Autowired
    private MetadataDAO metadataDAO;
	
	
	@Autowired
	public CPathWarehouseImpl() {
		// TODO Auto-generated constructor stub
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

	/* MetadataDAO methods */

    /**
     * (non-Javadoc)
     * @see cpath.warehouse.metadata.MetadataDAO#importMetadata;
     */
	public void importMetadata(final Metadata metadata) {
        metadataDAO.importMetadata(metadata);
    }

    /**
     * (non-Javadoc)
     * @see cpath.warehouse.metadata.MetadataDAO#getByCV
     */
    public Metadata getByCV(final String cv) {
        return metadataDAO.getByCV(cv);
    }
}
