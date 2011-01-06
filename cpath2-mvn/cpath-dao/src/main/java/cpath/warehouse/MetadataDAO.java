package cpath.warehouse;

// imports
import cpath.dao.Reindexable;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.PathwayData;

import java.util.Collection;

/**
 * An interface which provides methods to persist and query provider metadata.
 */
public interface MetadataDAO  extends Reindexable {

    /**
     * Persists the given metadata object to the db.
     *
     * @param metadata Metadata
     */
    void importMetadata(final Metadata metadata);

    /**
     * This method returns the metadata object with the given Identifier.
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
    Collection<Metadata> getAll();

    /**
     * Persists the pathway data stored in the given pathway data object to the ware house db.
     *
     * @param pathwayData PathwayData
     */
    void importPathwayData(final PathwayData pathwayData);

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
     * @return
     */
    PathwayData getPathwayData(Integer pathway_id);
    
}
