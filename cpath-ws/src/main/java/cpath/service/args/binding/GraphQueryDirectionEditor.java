package cpath.service.args.binding;

import org.biopax.paxtools.query.algorithm.Direction;

import java.beans.PropertyEditorSupport;


/**
 * @author ozgun
 *
 */
public class GraphQueryDirectionEditor extends PropertyEditorSupport {
	
	@Override
	public void setAsText(String arg0)
	{
		Direction value = null;
		value = Direction.valueOf(arg0.trim().toUpperCase());
		setValue(value);
	}
}