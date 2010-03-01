package cpath.fetcher.common;

// imports
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

/**
 * Reads data from service.
 */
public interface ServiceReader {

    /**
     *  For the given url, returns a collection of Objects from cpath.warehouse.beans
     *
     * @param inputStream InputStream
     * @param Collection<T>
     * @throws IOException if an IO error occurs
     */
    <T> void readFromService(final InputStream inputStream, final Collection<T> toReturn) throws IOException;
}
