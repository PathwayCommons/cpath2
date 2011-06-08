package cpath.webservice.args;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import cpath.service.CPathService.OutputFormat;

public class Get {
	@NotNull(message="Illegal Output Format") 
	private OutputFormat format;
	// required at least one value
	@NotNull(message="Parameter 'uri' is required!")
	@NotEmpty(message="At least one URI has to be set!")
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
