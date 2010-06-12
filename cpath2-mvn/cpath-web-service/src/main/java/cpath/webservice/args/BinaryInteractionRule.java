/**
 ** Copyright (c) 2010 Memorial Sloan-Kettering Cancer Center (MSKCC)
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

package cpath.webservice.args;

import java.util.*;

import org.biopax.paxtools.io.sif.BinaryInteractionType;

/**
 * Enumeration for the SIF rules that also 
 * contains old names for backward compatibility.
 * 
 * @author rodche
 *
 */
public enum BinaryInteractionRule {
	
	ACTIVATES(BinaryInteractionType.ACTIVATES.name()),
	CO_CONTROL(BinaryInteractionType.CO_CONTROL.name()),
	COMPONENT_OF(BinaryInteractionType.COMPONENT_OF.name()),
	IN_SAME_COMPONENT(BinaryInteractionType.IN_SAME_COMPONENT.name()),
	INACTIVATES(BinaryInteractionType.INACTIVATES.name()),
	INTERACTS_WITH(BinaryInteractionType.INTERACTS_WITH.name()),
	METABOLIC_CATALYSIS(BinaryInteractionType.METABOLIC_CATALYSIS.name()),
	REACTS_WITH(BinaryInteractionType.REACTS_WITH.name()),
	SEQUENTIAL_CATALYSIS(BinaryInteractionType.SEQUENTIAL_CATALYSIS.name()),
	STATE_CHANGE(BinaryInteractionType.STATE_CHANGE.name()),
	
	// for backward compatibility -
	CO_CONTROL_DEPENDENT_ANTI(BinaryInteractionType.CO_CONTROL.name()),
	CO_CONTROL_DEPENDENT_SIMILAR(BinaryInteractionType.CO_CONTROL.name()),
	CO_CONTROL_INDEPENDENT_ANTI(BinaryInteractionType.CO_CONTROL.name()),
	CO_CONTROL_INDEPENDENT_SIMILAR(BinaryInteractionType.CO_CONTROL.name()),
	CONTROLS_METABOLIC_CHANGE(BinaryInteractionType.METABOLIC_CATALYSIS.name()),
	COMPONENT_IN_SAME(BinaryInteractionType.IN_SAME_COMPONENT.name()),
	CONTROLS_STATE_CHANGE(BinaryInteractionType.STATE_CHANGE.name()),
	PARTICIPATES_CONVERSION(BinaryInteractionType.REACTS_WITH.name()),
	PARTICIPATES_INTERACTION(BinaryInteractionType.STATE_CHANGE.name()),
	;
	
	/**
	 * actual (paxtools's) binary interaction rule name
	 */
	public final String ruleName;
	
	private BinaryInteractionRule(String rule) {
		this.ruleName = rule;
	}
	
	private static final Set<String> allnames = new HashSet<String>();
	private static final Set<String> actualnames = new HashSet<String>();

	static {
		for(BinaryInteractionRule r : EnumSet.allOf(BinaryInteractionRule.class))
			allnames.add(r.name());
		
		for(BinaryInteractionType t : EnumSet.allOf(BinaryInteractionType.class))
			actualnames.add(t.name());
	}

	/**
	 * Gets all the SIF rules (names), including old legacy ones.
	 * 
	 * @return
	 */
	public static Set<String> allNames() {
		return Collections.unmodifiableSet(allnames);
	}
	
	/**
	 * Gets all the SIF rules (names)
	 * 
	 * @return
	 */
	public static Set<String> actualNames() {
		return Collections.unmodifiableSet(actualnames);
	}
}
