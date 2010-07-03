package cpath.fetcher;

// imports
import cpath.warehouse.beans.Metadata;


import java.io.IOException;

import org.biopax.paxtools.model.Model;

/**
 * Warehouse Data service.  Retrieves protein and small molecule data on behalf of warehouse.
 */
public interface WarehouseDataService {

    /**
     * For the given Metadata, converts target data int EntityReference objects and 
	 * adds into given paxtools model.
     *
	 * @param metadata Metadata
     * @param model Model
     * @throws IOException if an IO error occurs
     */
    void storeWarehouseData(final Metadata metadata, final Model model) throws IOException;
}
