package cpath.webservice;

public enum OutputFormat {
	BIOPAX,
	SIF,
	SBML,
	GSEA,
	PC_GENE_SET,
	ID_LIST,
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
