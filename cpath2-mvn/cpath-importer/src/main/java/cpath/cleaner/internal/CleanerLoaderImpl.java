package cpath.importer.cleaner.internal;

// imports
import cpath.importer.cleaner.Cleaner;
import cpath.importer.cleaner.CleanerLoader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Retrieves cleaner interface/cleaner class code.
 */
public final class CleanerLoaderImpl extends ClassLoader implements CleanerLoader {

	// logger
    private static Log log = LogFactory.getLog(CleanerLoaderImpl.class);

    /**
	 * Default Constructor.
	 */
	public CleanerLoaderImpl() {}

	/**
	 * (non-Javadoc)
	 * @see cpath.importer.cleaner.CleanerLoader#getCleaner(java.lang.byte[])
	 */
	public Cleaner getCleaner(final byte[] cleanerData) {

		try {
			Class cleanerClass = getClass(cleanerData);
			return (cleanerClass == null) ?
				null : (Cleaner)cleanerClass.newInstance();
		}
		catch (IllegalAccessException e) {
			return null;
		}
		catch (InstantiationException e) {
			return null;
		}
	}

    /**
     * Given byte[] which represents precompiled byte code,
	 * define and return a java class.
	 *
	 * @param cleanerData byte[]
	 * @return Class
     */
	private Class getClass(final byte[] cleanerData) {

		if (cleanerData == null) {
			log.info("findClass(), byte[] data is null");
		}
		
        // we don't care about class name,
		// we know it implements a Cleaner interface
		return (cleanerData == null) ?
			null : defineClass(null, cleanerData, 0, cleanerData.length);
	}
}
