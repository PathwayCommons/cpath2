package cpath.converter.internal;

// imports
import cpath.converter.Converter;
import static cpath.importer.internal.NormalizerImpl.*;

import org.biopax.paxtools.model.Model;

import java.io.InputStream;

/**
 * General implementation of Converter interface.
 */
public class BaseConverterImpl implements Converter {

	// use for, e.g., xref's RDFId = L3_UNIFICATIONXREF_URI + URLEncoder.encode(db + "_" +  id);
	public static final String L3_UNIFICATIONXREF_URI = BIOPAX_URI_PREFIX + "UnificationXref:";
	public static final String L3_PUBLICATIONXREF_URI = BIOPAX_URI_PREFIX + "PublicationXref:";
	public static final String L3_RELATIONSHIPXREF_URI = BIOPAX_URI_PREFIX + "RelationshipXref:";
	
	public static final String L2_UNIFICATIONXREF_URI = BIOPAX_URI_PREFIX + "unificationXref:";
	public static final String L2_PUBLICATIONXREF_URI = BIOPAX_URI_PREFIX + "publicationXref:";
	public static final String L2_RELATIONSHIPXREF_URI = BIOPAX_URI_PREFIX + "relationshipXref:";
	
	/**
	 * (non-Javadoc>
	 * @see cpath.converter.Converter#convert(java.io.InputStream, org.biopax.paxtools.model.Model)
	 */
	public void convert(final InputStream is, final Model model) {}
}
