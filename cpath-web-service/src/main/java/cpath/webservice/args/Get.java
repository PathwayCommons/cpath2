package cpath.webservice.args;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.ArrayUtils;
import org.hibernate.validator.constraints.NotEmpty;

import cpath.service.OutputFormat;

import java.util.HashSet;
import java.util.Set;

public class Get {
	@NotNull(message="Illegal Output Format") 
	@Valid
	private OutputFormat format;
	// required at least one value
	@NotEmpty(message="Provide at least one URI.")
	private String[] uri;
	
	public Get() {
		format = OutputFormat.BIOPAX; // default
	}

	public OutputFormat getFormat() {
		return format;
	}

	public void setFormat(OutputFormat format) {
		this.format = format;
	}

	public String[] getUri() {
		return uri;
	}

	public void setUri(String[] uri) {
		Set<String> uris = new HashSet<String>(uri.length);
		for(String item : uri) {
			if(item.contains(",")) {
				//split by ',' ignoring spaces and empty values (between ,,)
				for(String id : item.split("\\s*,\\s*", -1))
					uris.add(id);
			} else {
				uris.add(item);
			}
		}
		this.uri = uris.toArray(new String[uris.size()]);
	}
}
