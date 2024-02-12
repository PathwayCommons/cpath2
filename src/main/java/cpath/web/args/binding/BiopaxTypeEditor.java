package cpath.web.args.binding;

import java.beans.PropertyEditorSupport;

import org.apache.commons.lang3.StringUtils;
import org.biopax.paxtools.controller.EditorMap;
import org.biopax.paxtools.controller.SimpleEditorMap;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;


/**
 * Convert a request parameter value to the BioPAX type.
 * 
 * @author rodche
 */
public class BiopaxTypeEditor extends PropertyEditorSupport {
	private static BioPAXFactory bioPAXFactory = BioPAXLevel.L3.getDefaultFactory();
	private static EditorMap editorMap = SimpleEditorMap.L3;
	
	/* (non-Javadoc)
	 * @see java.beans.PropertyEditorSupport#setAsText(java.lang.String)
	 */
	@Override
	public void setAsText(String type) {
		setValue(BiopaxTypeEditor.getSearchableBiopaxClassByName(type));
	}
	
	/**
	 * Check whether given 'type' (case-insensitive) is a BioPAX interface that
	 * has a non-abstract implementation.
	 * BioPAX type names are recognized regardless up/lowercase, e.g.:
	 *  ProteinReference, PROTEINREFERENCE, proteinreference, etc. should all work the same.
	 *
	 * @param type BioPAX type/interface name, e.g. "Pathway", "Provenance" (case-insensitive)
	 * @throws IllegalArgumentException when the input string is not blank but does not match any BioPAX type
	 * @return the BioPAX interface; null when 'type' is null/blank
	 */
	public static Class<? extends BioPAXElement> getSearchableBiopaxClassByName(String type)
	{
		if(StringUtils.isBlank(type)) {
			return null;
		}

		for(Class<? extends BioPAXElement> c : editorMap.getKnownSubClassesOf(BioPAXElement.class)) {
			if(c.getSimpleName().equalsIgnoreCase(type)) {
				if(c.isInterface() && bioPAXFactory.getImplClass(c) != null)
					return c; // interface
			}
		}
		throw new IllegalArgumentException("Unknown BioPAX type: " + type);
	}
}
