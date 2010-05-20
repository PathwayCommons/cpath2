package cpath.webservice;

public enum OutputFormat {
	BIOPAX,
	SIF,
	SBML,
	GSEA,
	GENESET,
	TSV,
	IMAGE,
	;
	
	public static OutputFormat parseFormat(String value) {
		for(OutputFormat v : OutputFormat.values()) {
			if(value.equalsIgnoreCase(v.toString())) {
				return v;
			}
		}
		return null;
	}
}
