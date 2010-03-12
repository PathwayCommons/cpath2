package cpath.cleaner;

// imports

/**
 * Retrieves/instantiates cleaner interface/class code.
 */
public interface CleanerLoader {

	/**
	 * For the given precompiled java byte code,
	 * returns an instance of a class which
	 * implements the cleaner interface.
	 *
	 * @param cleanerData byte[]
	 * @return Cleaner
	 */
	Cleaner getCleaner(final byte[] cleanerData);
}
