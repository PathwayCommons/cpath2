package cpath.service;

/**
 * Enumeration of cPath service output formats
 * 
 * @author rodche
 *
 */
public enum OutputFormat {
    BIOPAX("BioPAX RDF/XML Format"),
	BINARY_SIF("Simple Binary Interactions Format"),
    EXTENDED_BINARY_SIF("Extended Simple Binary Interactions Format"),
	GSEA("Gene Set Expression Analysis Format"),
    SBGN("Systems Biology Graphical Notation Format")
	;
    
    private final String info;
    
    public String getInfo() {
		return info;
	}
    
    private OutputFormat(String info) {
		this.info = info;
	}
}