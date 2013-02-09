/**
 * 
 */
package cpath.dao;

import com.mchange.util.AssertException;

import cpath.warehouse.beans.ChemMapping;
import cpath.warehouse.beans.GeneMapping;

/**
 * 
 * @author rodche
 */
public final class IdMappingFactory {
	
	private IdMappingFactory() {
		throw new AssertException("Not instantiable");
	}

	
	/**
	 * Creates a new unmodifiable id-mapping bean.
	 * 
	 * @param type
	 * @param db
	 * @param id
	 * @return
	 * @throws IllegalArgumentException when type is unknown
	 */
	public static final IdMapping newIdMapping(Class<? extends IdMapping> type, String db, String id) {
		
		IdMapping m = null;
		
		if(type.equals(GeneMapping.class))
			m = new GeneMapping(db, id);
		else if(type.equals(ChemMapping.class))
			m = new ChemMapping(db, id);
		else
			throw new IllegalArgumentException("unknown type: " + type);
		
		return m;
	}
	
	
    /**
     * This method uses knowledge about the id-mapping
     * internal design (id -> to primary id table, no db names used) 
     * and about some of bio identifiers to increase the possibility
     * of successful (not null) mapping result.
     * 
     * Notes.
     * TODO
     * Might in the future, if we store all mappings in the same table, 
     * and therefore have to deal with several types of numerical identifiers,
     * which requires db name to be part of the primary key, this method can 
     * shield from the implementation details of making the key (i.e, we might 
     * want to use pubchem:123456, SABIO-RK:12345 as pk, despite the prefixes
     * are normally not part of the identifier).
     * 
     * @param db
     * @param id
     * @return suggested id to be used in a id-mapping call
     */
    public static String suggest(String db, String id) {
    	
    	if(db == null || db.isEmpty() || id == null || id.isEmpty())
    		return id;
    	
		// our warehouse-specific hack for matching uniprot.isoform, kegg, refseq xrefs,
		// e.g., ".../uniprot.isoform/P04150-2" becomes  ".../uniprot/P04150"
		if(db.equalsIgnoreCase("uniprot isoform") || db.equalsIgnoreCase("uniprot.isoform")) {
			int idx = id.lastIndexOf('-');
			if(idx > 0)
				id = id.substring(0, idx);
//			db = "UniProt";
		}
		else if(db.equalsIgnoreCase("refseq")) {
			//also want refseq:NP_012345.2 to become refseq:NP_012345
			int idx = id.lastIndexOf('.');
			if(idx > 0)
				id = id.substring(0, idx);
		} 
		else if(db.toLowerCase().startsWith("kegg") && id.matches(":\\d+$")) {
			int idx = id.lastIndexOf(':');
			if(idx > 0)
				id = id.substring(idx + 1);
//			db = "Entrez Gene";
		}
    		
    	return id;
    }


    /**
     * Gets the name of the stored named db query: 
     * "get all ids mapped to the primary id"
     * 
     * @param type
     * @return
     * @throws IllegalArgumentException when type is unknown
     */
	public static String getAllMappingsQueryName(Class<? extends IdMapping> type) {
		String queryName = null;
		
		if(type.equals(GeneMapping.class))
			queryName = "cpath.warehouse.beans.allGeneMapping";
		else if(type.equals(ChemMapping.class))
			queryName = "cpath.warehouse.beans.allChemMapping";
		else
			throw new IllegalArgumentException("unknown type: " + type);
		
		return queryName;
	}
	
	
	
	
}
