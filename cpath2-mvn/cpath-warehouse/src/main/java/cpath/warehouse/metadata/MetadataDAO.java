package cpath.warehouse.metadata;

// imports
import cpath.warehouse.beans.Metadata;

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
     * This method returns the metadata object with the given cv.
	 *
     * @param cv String
     * @return Metadata
     */
    Metadata getByCV(final String cv);
}