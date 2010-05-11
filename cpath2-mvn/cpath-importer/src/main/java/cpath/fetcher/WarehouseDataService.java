package cpath.fetcher;

// imports
import cpath.warehouse.beans.Metadata;

import org.biopax.paxtools.model.Model;

import java.io.IOException;

/**
 * Warehouse Data service.  Retrieves protein and small molecule data on behalf of warehouse.
 */
public interface WarehouseDataService {

    /**
     * For the given Metadata, returns a collection of EntityReference objects
	 * in a paxtools model.
     *
	 * @param metadata Metadata
     * @return Model
     * @throws IOException if an IO error occurs
     */
    Model getWarehouseData(final Metadata metadata) throws IOException;
}
