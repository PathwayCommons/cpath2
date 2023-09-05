package cpath.service.metadata;


import java.util.List;

/**
 * BIO ID-mapping.
 * 
 * @author rodche
 */
public interface Mappings {
	String FIELD_SRCDB = "srcDb";
	String FIELD_SRCID = "srcId";
	String FIELD_DSTDB = "dstDb";
	String FIELD_DSTID = "dstId";
	String FIELD_DOCID = "docId";

	List<Mapping> findByDstDbIgnoreCaseAndDstId(String dstDb, String dstId);

	List<Mapping> findBySrcIdInAndDstDbIgnoreCase(List<String> srcIds, String dstDb);

	void save(Mapping mapping);

	void commit();

	void refresh();

	void close();

	boolean isClosed();

	/**
	 * Total number of search hits for the given lucene query.
	 * @param queryString
	 * @return
	 */
	long count(String queryString);
}
