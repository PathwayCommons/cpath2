package cpath.warehouse;

// imports
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
     * Persists the given metadata object to the db.
     *
     * @param metadata Metadata
     */
    void importMetadata(final Metadata metadata);

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
     * Persists the pathway data stored in the given pathway data object to the warehouse db.
     *
     * @param pathwayData PathwayData
     */
    void importPathwayData(final PathwayData pathwayData);

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
     * This method returns the pathway data objects with the given Identifier and Version
	 *
     * @param identifier String
     * @param version Float
     * @return Collection<PathwayData>
     */
    Collection<PathwayData> getPathwayDataByIdentifierAndVersion(final String identifier, final String version);

    /**
     * This method returns a pathway data object with the given Identifier, Version, Filename and Digest.
	 *
     * @param identifier String
     * @param version Float
	 * @param filename String
	 * @param digest String
     * @return PathwayData
     */
    PathwayData getPathwayDataByIdentifierAndVersionAndFilenameAndDigest(final String identifier, final String version, final String filename, final String digest);

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
}
