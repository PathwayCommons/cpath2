package cpath.converter.internal;

/**
 * Implementation of Converter interface for PubChem data.
 */
public class PubChemConverterImpl extends BaseSDFConverterImpl {

	// some statics
	private static final String ENTRY_START = "M  END";
	private static final String ENTRY_END = "$$$$";

	/**
	 * Constructor.
	 */
	public PubChemConverterImpl() {
		super(SDFUtil.SOURCE.PUBCHEM, ENTRY_START, ENTRY_END);
	}
}
