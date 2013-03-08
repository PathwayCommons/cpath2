package cpath.warehouse;

// imports
import cpath.dao.IdMapping;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.PathwayData;

import java.util.Collection;
import java.util.Map;

import org.biopax.validator.api.beans.ValidatorResponse;

/**
 * An interface which provides methods to persist and query provider metadata.
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
     * This method returns the metadata object, given the {@link Metadata} Identifier.
	 *
     * @param identifier String
     * @return Metadata
     */
    Metadata getMetadataByIdentifier(final String identifier);

    /**
     * This method returns all metadata objects in warehouse.
	 *
     * @return Collection<Metadata>
     */
    Collection<Metadata> getAllMetadata();

    /**
     * Saves or updates the pathway data object in the metadata db.
     *
     * @param pathwayData PathwayData
     */
    void savePathwayData(final PathwayData pathwayData);

    /**
     * This method returns all the pathway data.
	 *
     * @return Collection<PathwayData>
     */
    Collection<PathwayData> getAllPathwayData();
    
    /**
     * This method returns the pathway data objects with the given Identifier.
	 *
     * @param identifier String
     * @return Collection<PathwayData>
     */
    Collection<PathwayData> getPathwayDataByIdentifier(final String identifier);


    /**
     * This method gets a PathwayData bean (initialized) by primary key.
     * 
     * @param pathway_id PK
     * @return PathwayData
     */
    PathwayData getPathwayData(final Integer pathway_id);
    
    
	/**
	 * Generates the BioPAX validation report for the pathway data provider.
	 * 
	 * @param metadataIdentifier datasource identifier, {@link Metadata#getIdentifier()} 
	 * @return
	 */
    ValidatorResponse getValidationReport(String metadataIdentifier);
	

	/**
	 * Generates the BioPAX validation report for a pathway data file.
	 * 
	 * @param pathwayDataPk a primary key value from the pathwatData table {@link PathwayData}
	 * @return
	 */
    ValidatorResponse getValidationReport(Integer pathwayDataPk);

    
	/**
	 * Gets a map (pk -> info) of {@link PathwayData} for the data source.
	 * 
	 * @param metadataIdentifier
	 * @return
	 */
    Map<Integer, String> getPathwayDataInfo(String metadataIdentifier);
    
    
    /**
     * Persists the gene/protein id map. 
     * 
     * @param idMap
     * @param type
     */
    void importIdMapping(Map<String, String> idMap, Class<? extends IdMapping> type);
    
    
    /**
     * Gets the mapping record (to the primary accession) 
     * by gene's other name or id.
     * 
     * @param db a standard db/resource name (id type) or null (it's optional really)
     * @param id identifier to be mapped
     * @param type
     * @return
     */
    IdMapping getIdMapping(String db, String id, Class<? extends IdMapping> type);
 
        
    /**
     * Gets all id-mapping records.
     * 
     * @param type
     * @return
     */
    Collection<IdMapping> getAllIdMappings(Class<? extends IdMapping> type);
 
    
    /**
     * Gets all mapping records as hash map
     * (name/id -> primary id).
     * 
     * @param type
     * @return
     */
    Map<String,String> getIdMap(Class<? extends IdMapping> type);

}
