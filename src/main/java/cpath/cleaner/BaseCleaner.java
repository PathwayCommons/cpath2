package cpath.cleaner;

import org.apache.commons.lang3.StringUtils;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.RelationshipXref;
import org.biopax.paxtools.model.level3.Xref;

final class BaseCleaner {
	
	private BaseCleaner() {
		throw new AssertionError("Not instantiable");
	}
	
	static RelationshipXref getOrCreateRx(Xref x, Model model) {
		final String xmlbase = StringUtils.isNotBlank(model.getXmlBase()) ? model.getXmlBase() : "";
		String id = x.getId();
		if(x.getIdVersion() != null) id += "." + x.getIdVersion();
		String db = x.getDb();
		if(x.getDbVersion() != null) db += "." + x.getDbVersion();
		String uri = xmlbase + "RX_" + encode(db + "_"+ id);
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
