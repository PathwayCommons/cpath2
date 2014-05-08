package cpath.dao;


import cpath.warehouse.beans.Metadata;

import java.util.List;

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
     * @return
     */
	Metadata saveMetadata(Metadata metadata);
    
    
	/**
	 * Deletes the Metadata (data source) and all 
	 * corresponding pathway data entries and generated files, 
	 * if any, from the db and filesystem.
	 * 
	 * @param metadata
	 */
	void deleteMetadata(Metadata metadata);
	
	
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
	 * @param provider data source (Metadata) identifier, not null
	 * @param file - base filename as in {@link Content}, or null (for all files)
	 * @return
	 */
    ValidatorResponse validationReport(String provider, String file);
    
    
	/**
	 * Imports metadata description from the resource.
	 * @param location
	 */
	void addOrUpdateMetadata(String location);


	/**
	 * Removes from the system all entries and 
	 * previously converted / premerged / validated 
	 * files accociated with this data provider.
	 * 
	 * @param metadata
	 * @return updated/saved object
	 */
	Metadata init(Metadata metadata);

}
