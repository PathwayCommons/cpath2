package cpath.protein;

// imports
import cpath.warehouse.beans.Metadata;

import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.EntityReference;

import java.io.IOException;

/**
 * Provider Protein Data service.  Retrieves provider protein data.
 */
public interface ProviderProteinDataService {

    /**
     * For the given Metadata, returns a collection of EntityReference objects
	 * in a paxtools model.
     *
	 * @param metadata Metadata
     * @return Model
     * @throws IOException if an IO error occurs
     */
    Model getProviderProteinData(final Metadata metadata) throws IOException;
}
