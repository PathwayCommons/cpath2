package cpath.webservice.args;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import cpath.service.OutputFormat;

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
		this.uri = uri;
	}
}
