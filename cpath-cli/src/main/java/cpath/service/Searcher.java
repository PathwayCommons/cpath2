package cpath.service;

import org.biopax.paxtools.model.BioPAXElement;

import cpath.service.jaxb.SearchResponse;

public interface Searcher {
	/**
	* Full-text search for BioPAX elements.
	* 
    * @param query String (keywords or Lucene query string)
	* @param page hits page number (when the number of hits exceeds a threshold)
	* @param filterByType - class filter
	* @param datasources  - filter by datasource
	* @param organisms - filter by organism
	* @return ordered list of hits (by score)
    */
	SearchResponse search(String query, int page,
  		Class<? extends BioPAXElement> filterByType, String[] datasources, String[] organisms);

}
