package cpath.service.web.args.binding;

import java.beans.PropertyEditorSupport;

import org.biopax.paxtools.controller.EditorMap;
import org.biopax.paxtools.controller.SimpleEditorMap;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;


/**
 * Helps convert request PROVIDER_URL path values to a BioPAX type.
 * 
 * @author rodche
 *
 */
public class BiopaxTypeEditor extends PropertyEditorSupport {
	private static BioPAXFactory bioPAXFactory = BioPAXLevel.L3.getDefaultFactory();
	private static EditorMap editorMap = SimpleEditorMap.L3;
	
	/* (non-Javadoc)
	 * @see java.beans.PropertyEditorSupport#setAsText(java.lang.String)
	 */
	@Override
	public void setAsText(String type) {
		setValue(getSearchableBiopaxClassByName(type));
	}
	
	/*
	 * Check whether given 'type' (case insensitive) is a BioPAX interface that
	 * has a non-abstract implementation.
	 * BioPAX type names are recognized regardless up/lowercase, e.g.:
	 *  ProteinReference, PROTEINREFERENCE, proteinreference, etc. should all work the same.
	 */
	private static Class<? extends BioPAXElement> getSearchableBiopaxClassByName(String type) 
	{
		for(Class<? extends BioPAXElement> c : editorMap.getKnownSubClassesOf(BioPAXElement.class)) {
			if(c.getSimpleName().equalsIgnoreCase(type)) {
				if(c.isInterface() && bioPAXFactory.getImplClass(c) != null)
					return c; // interface
			}
		}
		throw new IllegalArgumentException("Illegal BioPAX class name '" + type);
	}
}
