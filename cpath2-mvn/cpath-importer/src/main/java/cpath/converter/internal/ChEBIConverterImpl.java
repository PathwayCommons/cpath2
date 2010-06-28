package cpath.converter.internal;

/**
 * Implementation of Converter interface for ChEBI data.
 */
public class ChEBIConverterImpl extends BaseSDFConverterImpl {

	// some statics
	private static final String ENTRY_START = "M  END";
	private static final String ENTRY_END = "$$$$";

	/**
	 * Constructor.
	 */
	public ChEBIConverterImpl() {
		super(SDFUtil.SOURCE.CHEBI, ENTRY_START, ENTRY_END);
	}
}
