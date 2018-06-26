package cpath.service.api;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Cleaner interface.
 * 
 * Can be implemented and used for a particular
 * biological data source, when some filtering 
 * and fixing are required, not waiting for the next
 * provider's official data release.
 *
 */
public interface Cleaner {

	/**
	 * To clean, filter, fix the data.
	 *
	 * @param data
	 * @param cleanedData - result; the stream must be closed inside this method.
	 */
	void clean(InputStream data, OutputStream cleanedData);
}
