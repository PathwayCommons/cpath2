package cpath.service.web.args.binding;

import java.beans.PropertyEditorSupport;

import cpath.service.api.GraphType;


/**
 * @author rodche
 *
 */
public class GraphTypeEditor extends PropertyEditorSupport {
	
	/* (non-Javadoc)
	 * @see java.beans.PropertyEditorSupport#setAsText(java.lang.String)
	 */
	@Override
	public void setAsText(String arg0) {
		setValue(GraphType.valueOf(arg0.trim().toUpperCase()));
	}
	
}
