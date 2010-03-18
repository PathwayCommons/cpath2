package cpath.warehouse.pathway;

// imports
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.PathwayData;

import org.biopax.paxtools.model.Model;

import java.util.Collection;

/**
 * An interface which provides methods to persist and query provider pathway data.
 */
public interface PathwayDataDAO {

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
    Collection<PathwayData> getByIdentifier(final String identifier);

    /**
     * This method returns the pathway data objects with the given Identifier and Version
	 *
     * @param identifier String
     * @param version Float
     * @return Collection<PathwayData>
     */
    Collection<PathwayData> getByIdentifierAndVersion(final String identifier, final Float version);

    /**
     * This method returns the pathway data objects with the given Identifier, Version, Filename and Digest.
	 *
     * @param identifier String
     * @param version Float
	 * @param filename String
	 * @param digest String
     * @return PathwayData
     */
    PathwayData getByIdentifierAndVersionAndFilenameAndDigest(final String identifier, final Float version, final String filename, final String digest);
}
