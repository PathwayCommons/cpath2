package cpath.importer;

// imports
import cpath.importer.internal.PremergeDispatcher;
import cpath.warehouse.beans.Metadata;


public interface Premerge {

	/**
	 * Called by dispatcher to set pointer back to himself
	 *
	 * @param premergeDispatcher PremergeDispatcher
	 */
	void setDispatcher(final PremergeDispatcher premergeDispatcher);

	/**
	 * Called by dispatcher to set metadata object ref. The object to process.
	 *
	 * @param metadata Metadata
	 */
	void setMetadata(final Metadata metadata);

	/**
	 * Called to start the premerge process.
	 */
	void premerge();
}
