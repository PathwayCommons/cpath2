package cpath.dao;


import cpath.warehouse.beans.Mapping;
import cpath.warehouse.beans.Metadata;

import java.util.List;
import java.util.Set;

import org.biopax.validator.api.beans.ValidatorResponse;

/**
 * An interface which provides methods 
 * to persist and query provider metadata,
 * processed biopax, validation and id-mapping data.
 */
public interface MetadataDAO {

    /**
     * Persists or updates the given metadata object to the db.
     *
     * @param metadata Metadata
     */
	void saveMetadata(Metadata metadata);
    
    
    /**
     * This method returns the metadata object, given the {@link Metadata} Identifier.
	 *
     * @param identifier String
     * @return Metadata
     */
    Metadata getMetadataByIdentifier(String identifier);

    
    /**
     * This method returns all metadata objects.
	 *
     * @return Collection<Metadata>
     */
    List<Metadata> getAllMetadata();
    
    
	/**
	 * Generates the BioPAX validation report for a pathway data file.
	 * 
	 * @param provider 
	 * @param file - base filename as in {@link PathwayData}
	 * @return
	 */
    ValidatorResponse validationReport(String provider, String file);
    
    
    /**
     * Persists the id-mapping entity.
     * @param mapping
     */
    void saveMapping(Mapping mapping);
    
    
    /**
     * Gets available mapping entities by type.
     * 
     * @param type
     * @return
     */
    List<Mapping> getMappings(Mapping.Type type);
    
    
    /**
     * Maps the identifier.
     * 
     * @param identifier
     * @param type
     * @param idType data collection name (it helps match id versions, \
     *        isoforms even if they're not stored in the mapping tables; can be null)
     * @return a set of primary IDs of the type; normally one or none elements
     */
    Set<String> mapIdentifier(String identifier, Mapping.Type type, String idType);


	/**
	 * Imports metadata description from the resource.
	 * @param location
	 */
	void addOrUpdateMetadata(String location);


	/**
	 * Removes fromthe system all entries and 
	 * previously converted / premerged / validated 
	 * files accociated with this data provider;
	 * then loads original data into memory.
	 * 
	 * @param metadata
	 */
	void init(Metadata metadata);
    
}
