package cpath.service.args.binding;

import java.beans.PropertyEditorSupport;

import org.biopax.paxtools.query.algorithm.Direction;


/**
 * @author rodche
 *
 */
public class DirectionEditor extends PropertyEditorSupport {
	
	/* (non-Javadoc)
	 * @see java.beans.PropertyEditorSupport#setAsText(java.lang.String)
	 */
	@Override
	public void setAsText(String arg0) {
		setValue(Direction.valueOf(arg0.trim().toUpperCase()));
	}
	
}
