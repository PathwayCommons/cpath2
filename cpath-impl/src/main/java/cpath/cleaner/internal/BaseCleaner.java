package cpath.cleaner.internal;

import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.RelationshipXref;
import org.biopax.paxtools.model.level3.UnificationXref;

final class BaseCleaner {
	
	private BaseCleaner() {
		throw new AssertionError("Not instantiable");
	}
	
	static RelationshipXref getOrCreateRxByUx(UnificationXref x, Model model) {
		final String xmlbase = (model.getXmlBase() != null) ? model.getXmlBase() : "";
		String id = x.getId();
		if(x.getIdVersion() != null) id += "." + x.getIdVersion();
		String db = x.getDb();
		if(x.getDbVersion() != null) db += "." + x.getDbVersion();
		String uri = xmlbase + "RelationshipXref_" + encode(db + "_"+ id);
		
		RelationshipXref rx = (RelationshipXref) model.getByID(uri);
		if(rx == null) { //make a new one
			rx = model.addNew(RelationshipXref.class, uri);
			rx.setDb(x.getDb());
			rx.setId(x.getId());
			rx.setIdVersion(x.getIdVersion());
			rx.setDbVersion(x.getDbVersion());
			rx.getComment().addAll(x.getComment());
		}
		return rx;
	}

	static String encode(String id) {
		return id.replaceAll("[^-\\w]", "_");
	}
}
