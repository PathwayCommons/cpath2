package cpath.webservice.args.binding;

import java.beans.PropertyEditorSupport;

import cpath.warehouse.beans.Metadata;


public class MetadataTypeEditor extends PropertyEditorSupport {
	
	@Override
	public void setAsText(String arg0) {
		Metadata.METADATA_TYPE value = null;
			value = Metadata.METADATA_TYPE.valueOf(arg0.trim().toUpperCase());
			setValue(value);
	}
	
}
