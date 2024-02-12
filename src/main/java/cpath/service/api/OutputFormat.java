package cpath.service.api;

import org.apache.commons.lang3.StringUtils;

/**
 * Pre-defined Output Formats.
 * 
 * @author rodche
 */
public enum OutputFormat {

	BIOPAX("BioPAX Level 3 RDF/XML Format",".owl","application/vnd.biopax.rdf+xml"),

	SIF("Simple Binary Interaction Format (was BINARY_SIF)",".sif","text/plain"),

	/**
	 * Depending on extra properties/parameters this can be anything,
	 * such as a CSV/TSV data;
	 * by default, it will be the Pathway Commons Extended Binary SIF using default parameters.
	 */
	TXT("Extended SIF (was EXTENDED_BINARY_SIF)",".txt","text/plain"),

	GSEA("Gene Set Expression Analysis Format",".gmt","text/plain"),

	SBGN("Systems Biology Graphical Notation Format",".sbgn.xml","application/xml"),

	JSONLD("JSON-LD format", ".json", "application/ld+json")
	;

  final String info;
	final String ext;
	final String mediaType;
    
  public String getInfo() {
		return info;
	}

	public String getExt() {
		return ext;
	}

	public String getMediaType() {
		return mediaType;
	}
    
	OutputFormat(String info, String ext, String mediaType) {
		this.info = info;
		this.ext = ext;
		this.mediaType = mediaType;
	}

	public static OutputFormat typeOf(String tag)
	{
		if(StringUtils.isBlank(tag))
			return null;

		OutputFormat type = null;
		try {
			type = valueOf(tag.toUpperCase());
		}
		catch (IllegalArgumentException e){}

		return type;
	}
}