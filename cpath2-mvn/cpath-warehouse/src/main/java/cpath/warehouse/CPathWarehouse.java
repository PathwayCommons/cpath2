package cpath.warehouse;

// imports
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.metadata.MetadataDAO;

import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.level3.UtilityClass;

import java.util.Set;

/**
 * cPathSquared Warehouse Interface
 * 
 * @author rodch
 *
 * TODO define warehouse methods
 */
public interface CPathWarehouse extends MetadataDAO {

    /* utility class methods */
	
	/**
	 * Creates a new standard reference BioPAX object 
	 * (e.g., CellVocabulary or ProteinReference)
	 * 
	 * @param <T> UtilityClass or its subclass (e.g., ProteinReference)
	 * @param urn
	 * @param clazz
	 * @return
	 * 
	 * TODO maybe, remove this one
	 */
	<T extends UtilityClass> T createUtilityClass(String primaryUrn, Class<T> utilityClazz);
	
	/**
	 * Creates a new standard reference BioPAX object 
	 * (e.g., CellVocabulary or ProteinReference),
	 * auto-resolving URN to the proper UtilityClass
	 * 
	 * @param <T> UtilityClass or its subclass (e.g., ProteinReference)
	 * @param urn
	 * @return
	 */
	<T extends UtilityClass> T createUtilityClass(String primaryUrn);
	
	/**
	 * Creates a new standard BioPAX element within the given context.
	 * 
	 * @param <T> EntityReference or subclass
	 * @param primaryUrn
	 * @param EntityReference
	 * @return
	 */
	<T extends UtilityClass> T createUtilityClass(String primaryUrn, Class<? extends BioPAXElement> domain, String property);
	
	/* miriam methods */

	/**
	 * Gets the primary ID (URN) of the BioPAX element, given its current URN.
	 * 
	 * @param urn
	 * @return
	 */
	String getPrimaryURI(String urn); // uses ID mapper
	
	/* controlled vocabularies methods */
	
	/**
	 * Gets primary IDs (URNs) of all the controlled vocabularies that
	 * are either indirect or direct children of the one identified by its URN.
	 * 
	 * @param urn a ControlledVocabulary Id
	 * @return set of ControlledVocabulary URNs
	 */
	Set<String> getAllChildrenOfCv(String urn);
	Set<String> getDirectChildrenOfCv(String urn);
	Set<String> getParentsOfCv(String urn);

	/* MetadataDAO methods */

    /**
     * (non-Javadoc)
     * @see cpath.warehouse.metadata.MetadataDAO#importMetadata;
     */
	void importMetadata(final Metadata metadata);

    /**
     * (non-Javadoc)
     * @see cpath.warehouse.metadata.MetadataDAO#getByCV
     */
    Metadata getByCV(final String cv);
}
