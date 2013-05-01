package cpath.dao;


import cpath.warehouse.beans.Mapping;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.PathwayData;

import java.util.Collection;
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
    void saveMetadata(final Metadata metadata);
    
    
    /**
     * Gets a Metadata bean by id (primary key).
     * 
     * @param id Metadata PK
     * @return
     */
    Metadata getMetadata(final Integer id);
    

    /**
     * Deletes the Metadata and its pathway data entries, 
     * if any (previously processed), from the db by id (primary key).
     * 
     * @param id Metadata PK
     * @return
     */
    void deleteMetadata(final Integer id);

    
    /**
     * Deletes the pathway data entry, 
     * from the db by id (primary key).
     * 
     * @param id PathwayData PK
     * @return
     */
    void deletePathwayData(final Integer id);
    
    
    /**
     * This method returns the metadata object, given the {@link Metadata} Identifier.
	 *
     * @param identifier String
     * @return Metadata
     */
    Metadata getMetadataByIdentifier(final String identifier);

    /**
     * This method quickly returns all metadata,
     * but the corresponding Metadata.pathwayData lazy collections 
     * might not be initialized yet, thus cannot be iterated over 
     * outside an active database session/transaction.
	 *
     * @return Collection<Metadata>
     */
    Collection<Metadata> getAllMetadata();

    
    /**
     * This method returns all metadata objects
     * and makes sure all (lazy) collections are initialized (loaded).
	 *
     * @return Collection<Metadata>
     */
    Collection<Metadata> getAllMetadataInitialized();


    /**
     * This method gets a PathwayData bean (initialized) by primary key.
     * 
     * @param pathway_id PK
     * @return PathwayData
     */
    PathwayData getPathwayData(final Integer pathway_id);
    	

    /**
     * This method gets a unique PathwayData bean (initialized) by 
     * both metadata identifier and data file name.
     * 
     * @param provider Metadata identifier (unique provider's internal id)
     * @param filename a file name of the pathway data record (usually, as it's read from the archive)
     * @return PathwayData
     */
    PathwayData getPathwayData(final String provider, final String filename);
    
    
	/**
	 * Generates the BioPAX validation report for a pathway data file.
	 * @param provider 
	 * 
	 * @param pathwayDataPk a primary key value from the pathwatData table {@link PathwayData}
	 * @return
	 */
    ValidatorResponse validationReport(String provider, Integer pathwayDataPk);
    
    
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
    
}
