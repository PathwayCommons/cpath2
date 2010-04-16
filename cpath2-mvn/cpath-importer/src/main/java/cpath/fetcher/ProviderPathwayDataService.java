package cpath.fetcher;

// imports
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.PathwayData;

import java.io.IOException;
import java.util.Collection;

/**
 * Provider PathwayData service.  Retrieves provider pathway data.
 */
public interface ProviderPathwayDataService {

    /**
     *  For the given Metadata, returns a collection of PathwayData objects.
     *
	 * @param metadata Metadata
     * @return Collection<PathwayData>
     * @throws IOException if an IO error occurs
     */
    Collection<PathwayData> getProviderPathwayData(final Metadata metadata) throws IOException;
}
