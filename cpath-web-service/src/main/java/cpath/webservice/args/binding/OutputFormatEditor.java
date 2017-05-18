package cpath.webservice.args.binding;

import java.beans.PropertyEditorSupport;

import cpath.service.OutputFormat;


/**
 * @author rodche
 *
 */
public class OutputFormatEditor extends PropertyEditorSupport {
	
	/* (non-Javadoc)
	 * @see java.beans.PropertyEditorSupport#setAsText(java.lang.String)
	 */
	@Override
	public void setAsText(String param) {
		setValue(OutputFormat.valueOf(param.trim().toUpperCase()));
	}	
}
