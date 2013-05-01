package cpath.webservice.args;

import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

public class GetProperty {
	@NotBlank(message="Property Path is blank (not specified).")
	private String path;
	// required at least one value
	@NotEmpty(message="Provide at least one URI.")
	private String[] uri;
	
	public GetProperty() {
	}

	public String[] getUri() {
		return uri;
	}

	public void setUri(String[] uri) {
		this.uri = uri;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
}
