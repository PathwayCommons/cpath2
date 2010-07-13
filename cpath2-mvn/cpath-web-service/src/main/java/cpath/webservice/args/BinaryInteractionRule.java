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
import org.biopax.paxtools.io.sif.level3.*;

/**
 * Enumeration for the SIF rules that also 
 * contains old names for backward compatibility.
 * 
 * @author rodche
 *
 */
public enum BinaryInteractionRule {
	
	ACTIVATES(BinaryInteractionType.ACTIVATES.name(), null),
	CO_CONTROL(BinaryInteractionType.CO_CONTROL.name(), new ControlsTogetherRule()),
	IN_SAME_COMPONENT(BinaryInteractionType.IN_SAME_COMPONENT.name(), new ComponentRule()),
	INACTIVATES(BinaryInteractionType.INACTIVATES.name(), null),
	INTERACTS_WITH(BinaryInteractionType.INTERACTS_WITH.name(), new ParticipatesRule()),
	METABOLIC_CATALYSIS(BinaryInteractionType.METABOLIC_CATALYSIS.name(), new ControlRule()),
	REACTS_WITH(BinaryInteractionType.REACTS_WITH.name(), new ParticipatesRule()),
	SEQUENTIAL_CATALYSIS(BinaryInteractionType.SEQUENTIAL_CATALYSIS.name(), new ConsecutiveCatalysisRule()),
	STATE_CHANGE(BinaryInteractionType.STATE_CHANGE.name(), new ControlRule()),
	
	// for backward compatibility -
	COMPONENT_OF(BinaryInteractionType.COMPONENT_OF.name(), null),
	CO_CONTROL_DEPENDENT_ANTI(BinaryInteractionType.CO_CONTROL.name(), new ControlsTogetherRule()),
	CO_CONTROL_DEPENDENT_SIMILAR(BinaryInteractionType.CO_CONTROL.name(), new ControlsTogetherRule()),
	CO_CONTROL_INDEPENDENT_ANTI(BinaryInteractionType.CO_CONTROL.name(), new ControlsTogetherRule()),
	CO_CONTROL_INDEPENDENT_SIMILAR(BinaryInteractionType.CO_CONTROL.name(), new ControlsTogetherRule()),
	CONTROLS_METABOLIC_CHANGE(BinaryInteractionType.METABOLIC_CATALYSIS.name(), new ControlRule()),
	COMPONENT_IN_SAME(BinaryInteractionType.IN_SAME_COMPONENT.name(), new ComponentRule()),
	CONTROLS_STATE_CHANGE(BinaryInteractionType.STATE_CHANGE.name(), new ControlRule()),
	PARTICIPATES_CONVERSION(BinaryInteractionType.REACTS_WITH.name(), new ParticipatesRule()),
	PARTICIPATES_INTERACTION(BinaryInteractionType.STATE_CHANGE.name(), new ControlRule()),
	;
	
	/**
	 * actual (paxtools's) binary interaction rule name
	 */
	public final String ruleName;
	public final InteractionRuleL3 rule;
	
	private BinaryInteractionRule(String ruleName, InteractionRuleL3 rule) {
		this.ruleName = ruleName;
		this.rule = rule;
	}
	
	private static final Set<String> allnames = new HashSet<String>();
	private static final Set<String> actualnames = new HashSet<String>();
	private static final Set<InteractionRuleL3> allrules = new HashSet<InteractionRuleL3>();

	static {
		for(BinaryInteractionRule r : EnumSet.allOf(BinaryInteractionRule.class))
		{
			allnames.add(r.name());
			actualnames.add(r.ruleName);
			if(r.rule != null)
				allrules.add(r.rule);
		}
	}

	/**
	 * Gets all the SIF rule names (as defined in the enumeration), 
	 * including old/legacy ones.
	 * 
	 * @return
	 */
	public static Set<String> allNames() {
		return Collections.unmodifiableSet(allnames);
	}
	
	/**
	 * Gets all the SIF rules (actual pxtools's names)
	 * 
	 * @return
	 */
	public static Set<String> actualNames() {
		return Collections.unmodifiableSet(actualnames);
	}

	
	/**
	 * Gets all the SIF rules.
	 * 
	 * @return
	 */
	public static Set<InteractionRuleL3> allRules() {
		return Collections.unmodifiableSet(allrules);
	}
}
