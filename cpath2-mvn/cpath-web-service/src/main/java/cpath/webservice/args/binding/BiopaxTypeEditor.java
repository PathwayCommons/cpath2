/**
 ** Copyright (c) 2009 Memorial Sloan-Kettering Cancer Center (MSKCC)
 ** and University of Toronto (UofT).
 **
 ** This is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** any later version.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** both UofT and MSKCC have no obligations to provide maintenance, 
 ** support, updates, enhancements or modifications.  In no event shall
 ** UofT or MSKCC be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** UofT or MSKCC have been advised of the possibility of such damage.  
 ** See the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this software; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA;
 ** or find it at http://www.fsf.org/ or http://www.gnu.org.
 **/

package cpath.webservice.args.binding;

import java.beans.PropertyEditorSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.controller.EditorMap;
import org.biopax.paxtools.controller.SimpleEditorMap;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;


/**
 * Helps convert request URL path values to a BioPAX type.
 * 
 * @author rodche
 *
 */
public class BiopaxTypeEditor extends PropertyEditorSupport {
	private static BioPAXFactory bioPAXFactory = BioPAXLevel.L3.getDefaultFactory();
	private static EditorMap editorMap = SimpleEditorMap.L3;
	private static final Log log = LogFactory.getLog(BiopaxTypeEditor.class);
	
	/* (non-Javadoc)
	 * @see java.beans.PropertyEditorSupport#setAsText(java.lang.String)
	 */
	@Override
	public void setAsText(String type) {
		setValue(getSearchableBiopaxClassByName(type));
	}
	
	/*
	 * Enables using arguments, BioPAX class names, in any case: 
	 *     ProteinReference, PROTEINREFERENCE, proteinreference, etc.
	 * 
	 */
	private static Class<? extends BioPAXElement> getSearchableBiopaxClassByName(String type) {
		// case sensitive -
		//return bioPAXFactory.getImplClass(BioPAXLevel.L3.getInterfaceForName(type));
		
		// better - case insensitive way -
		for(Class<? extends BioPAXElement> c : editorMap.getKnownSubClassesOf(BioPAXElement.class)) {
			if(c.getSimpleName().equalsIgnoreCase(type)) {
				//if(bioPAXFactory.canInstantiate(c)) //does not matter if abstract (can still search by)
					//return bioPAXFactory.getImplClass(c);
				if(c.isInterface() && bioPAXFactory.getImplClass(c) != null)
					return c; // interface!
			}
		}
		
		log.info("Illegal BioPAX class name '" +
			type + "' (cannot this " +
			"as a filter by type value in queries) ");
		return null;
	}
}
