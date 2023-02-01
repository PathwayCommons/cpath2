package cpath.web.args.binding;

import java.beans.PropertyEditorSupport;

import cpath.service.metadata.Datasource;


public class MetadataTypeEditor extends PropertyEditorSupport {
	
	@Override
	public void setAsText(String arg0) {
		Datasource.METADATA_TYPE value = Datasource.METADATA_TYPE.valueOf(arg0.trim().toUpperCase());
		setValue(value);
	}
	
}
