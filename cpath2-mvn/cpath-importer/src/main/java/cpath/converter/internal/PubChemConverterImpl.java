package cpath.converter.internal;

/**
 * Implementation of Converter interface for PubChem data.
 */
public class PubChemConverterImpl extends BaseSDFConverterImpl {

	// some statics
	private static final String PUBCHEM_NAMESPACE_PREFIX = "http://ebi.org#";
	private static final String ENTRY_START = "M  END";
	private static final String ENTRY_END = "$$$$";

	/**
	 * Constructor.
	 */
	public PubChemConverterImpl() {
		super(SDFUtil.SOURCE.PUBCHEM, PUBCHEM_NAMESPACE_PREFIX, ENTRY_START, ENTRY_END);
	}
}
