package cpath.jpa;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

/**
 * Additional Mapping DAO methods and tuning.
 * 
 * @author rodche
 *
 */
public class MappingsRepositoryImpl implements MappingsRepositoryCustom {

	private static final Logger log = LoggerFactory.getLogger(MappingsRepositoryImpl.class);
	
	@Autowired //spring-data base repository (CRUD and findBy* methods)
	private MappingsRepository mappingsRepository; 
	

	/* (non-Javadoc)
	 * @see cpath.jpa.MappingsRepositoryCustom#map(java.lang.String, java.lang.String, java.lang.String)
	 */
	public Set<String> map(String fromDb, String fromId, String toDb) {
    	Assert.hasText(fromId);
    	Assert.hasText(toDb);
    	
    	List<Mapping> maps;    	
    	if(fromDb == null) {
    		maps = mappingsRepository.findBySrcIdAndDestIgnoreCase(fromId, toDb);
    	} else {    	
    		//if possible, use a "canonical" id instead isoform, version, kegg gene...
    		// (e.g., uniprot.isoform, P04150-2 pair becomes uniprot, P04150)
    		String id = fromId;
    		String db = fromDb;
    		
    		if(db.equalsIgnoreCase("uniprot isoform") 
    				|| db.equalsIgnoreCase("uniprot.isoform")) 
    		{
    			int idx = id.lastIndexOf('-');
    			if(idx > 0) {//using corr. UniProt ID instead
    				id = id.substring(0, idx);
    				
    			}
    		}
    		else if(db.toUpperCase().startsWith("UNIPROT")) {
    			//e.g., 'UNIPROT' instead of 'UniProt Knowledgebase'
    			db = "UNIPROT";
    		} 
    		else if(db.equalsIgnoreCase("refseq")) {
    			//strip, e.g., refseq:NP_012345.2 to refseq:NP_012345
    			int idx = id.lastIndexOf('.');
    			if(idx > 0)
    				id = id.substring(0, idx);
    		} 
    		else if(db.toLowerCase().startsWith("kegg") && id.matches(":\\d+$")) {
    			int idx = id.lastIndexOf(':');
    			if(idx > 0) {
    				id = id.substring(idx + 1); //it's NCBI Gene ID;
    				db = "NCBI GENE";
    			}
    		}
    		else if(db.equalsIgnoreCase("GENEID") || db.equalsIgnoreCase("ENTREZ GENE")) {
    			db = "NCBI GENE";
    		}
    		
    		maps = mappingsRepository
    			.findBySrcIgnoreCaseAndSrcIdAndDestIgnoreCase(db, id, toDb);
    		
    		if(maps.isEmpty()) {
    			maps = mappingsRepository.findBySrcIdAndDestIgnoreCase(id, toDb);
    			if(!maps.isEmpty()) {
    				log.warn("map(): after ignoring (srcDb) " + db + 
    					", managed to map " + id + " to " + maps);
    			}
    		}
    	}
    	
    	Set<String> results = new TreeSet<String>();   	
    	for(Mapping m : maps) {
    		results.add(m.getDestId());
    	}
    	
		return results;
	}


	/* (non-Javadoc)
	 * @see cpath.jpa.MappingsRepositoryCustom#map(java.lang.String)
	 */
	public Set<String> map(String identifier) {
		if(identifier.startsWith("http://"))
			throw new AssertionError("URI is not allowed here; use ID");
		
		if(identifier.toUpperCase().startsWith("CHEBI:")) {
			// chebi -> to primary chebi id
			return map("CHEBI", identifier, "CHEBI");
		} else if(identifier.length() == 25 || identifier.length() == 27) {
			// InChIKey identifier (25 or 27 chars long) -> to primary chebi id
			return map(null, identifier, "CHEBI"); //null - for looking in InChIKey, names, etc.
		} else if(identifier.toUpperCase().startsWith("CID:")) {
			// - a hack to tell PubChem ID from NCBI Gene ID in graph queries
			return map("PubChem-compound", identifier.substring(4), "CHEBI");
		} else if(identifier.toUpperCase().startsWith("SID:")) {
			// - a hack to tell PubChem ID from NCBI Gene ID in graph queries
			return map("PubChem-substance", identifier.substring(4), "CHEBI");
		} else if(identifier.toUpperCase().startsWith("PUBCHEM:")) { 
			// - a hack to tell PubChem ID from NCBI Gene ID in graph queries
			return map("PubChem-compound", identifier.substring(8), "CHEBI");	
		} else {
			// gene/protein name, id, etc. -> to primary uniprot AC
			Set<String> ret = new TreeSet<String>();
			ret.addAll(map(null, identifier, "UNIPROT"));
			if(ret.isEmpty()) //ChEMBL, DrugBank, chem. names, etc to ChEBI
				ret.addAll(map(null, identifier, "CHEBI"));
			return ret;
		}
	}	
	
}
