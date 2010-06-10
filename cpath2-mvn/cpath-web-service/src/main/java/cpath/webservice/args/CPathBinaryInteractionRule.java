package cpath.webservice.args;

import java.util.HashSet;
import java.util.Set;

import org.biopax.paxtools.io.sif.BinaryInteractionType;

/**
 * Enumeration for the SIF rules that also 
 * contains legacy names for backward compatibility.
 * 
 * @author rodche
 *
 */
public enum CPathBinaryInteractionRule {
	
	ACTIVATES(BinaryInteractionType.ACTIVATES.name()),
	CO_CONTROL(BinaryInteractionType.CO_CONTROL.name()),
	COMPONENT_OF(BinaryInteractionType.COMPONENT_OF.name()),
	IN_SAME_COMPONENT(BinaryInteractionType.IN_SAME_COMPONENT.name()),
	INACTIVATES(BinaryInteractionType.INACTIVATES.name()),
	INTERACTS_WITH(BinaryInteractionType.INTERACTS_WITH.name()),
	METABOLIC_CATALYSIS(BinaryInteractionType.METABOLIC_CATALYSIS.name()),
	REACTS_WITH(BinaryInteractionType.REACTS_WITH.name()),
	SEQUENTIAL_CATALYSIS(BinaryInteractionType.SEQUENTIAL_CATALYSIS.name()),
	
	// legacy/deprecated names
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
	
	private CPathBinaryInteractionRule(String rule) {
		this.ruleName = rule;
	}
	
	public static CPathBinaryInteractionRule parse(String value) {
		for(CPathBinaryInteractionRule v : CPathBinaryInteractionRule.values()) {
			if(value.equalsIgnoreCase(v.toString())) {
				return v;
			}
		}
		return null;
	}
	
	public Set<String> names() {
		Set<String> names = new HashSet<String>();
		for(CPathBinaryInteractionRule rule : CPathBinaryInteractionRule.values()) {
			names.add(rule.name());
		}
		return names;
	}
}
