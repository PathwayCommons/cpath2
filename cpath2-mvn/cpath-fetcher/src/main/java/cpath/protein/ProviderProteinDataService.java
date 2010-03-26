package cpath.protein;

// imports
import cpath.warehouse.beans.Metadata;

import org.biopax.paxtools.model.level3.EntityReference;

import java.io.IOException;
import java.util.Collection;

/**
 * Provider Protein Data service.  Retrieves provider protein data.
 */
public interface ProviderProteinDataService {

    /**
     *  For the given Metadata, returns a collection of EntityReference objects.
     *
	 * @param metadata Metadata
     * @return Collection<PathwayData>
     * @throws IOException if an IO error occurs
     */
    Collection<EntityReference> getProviderProteinData(final Metadata metadata) throws IOException;
}
