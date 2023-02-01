package cpath.service.metadata;

import org.biopax.paxtools.model.BioPAXElement;

import cpath.service.jaxb.SearchResponse;
import org.biopax.paxtools.model.Model;

public interface Index {
	// search fields
	String FIELD_URI = "uri";
	String FIELD_KEYWORD = "keyword"; //anything, e.g., names, terms, comments from child elements including
	String FIELD_NAME = "name"; // standardName, displayName, other names
	String FIELD_XREFID = "xrefid"; //xref.id
	String FIELD_PATHWAY = "pathway"; //pathways and parent pathways to be inferred from entire biopax model
	String FIELD_N_PARTICIPANTS = "participants"; //num. of PEs or Genes in a process or Complex
	String FIELD_N_PROCESSES = "processes"; //no. bio processes (aka size)

	//Full-text search/filter fields (case-sensitive);
	//index organism names, cell/tissue type (CV term), taxonomy id, but store only BioSource URIs
	String FIELD_ORGANISM = "organism";
	//index data source names, but only URIs are stored in the index
	String FIELD_DATASOURCE = "datasource";
	String FIELD_TYPE = "type";

	//Default fields to use with the MultiFieldQueryParser;
	//one can still search in other fields directly, like - pathway:some_keywords datasource:"pid"
	String[] DEFAULT_FIELDS =
			{
					FIELD_KEYWORD, //data type properties (name, id, term, comment) of this and child elements;
					FIELD_XREFID,
					FIELD_NAME
			};

	void setMaxHitsPerPage(int maxHitsPerPage);
	int getMaxHitsPerPage();
	
	/**
	* Full-text search for an object.
	* 
    * @param query String (keywords or Lucene query string)
	* @param page hits page number (when the number of hits exceeds a threshold)
	* @param type - filter by class
	* @param datasources  - filter by datasource
	* @param organisms - filter by organism
	* @return ordered list of hits (by score)
    */
	SearchResponse search(String query, int page, Class<? extends BioPAXElement> type, String[] datasources, String[] organisms);

	void save(BioPAXElement bpe);

	void save(Model model);

	void commit();

	void close();

	void refresh();

	boolean isClosed();
}
