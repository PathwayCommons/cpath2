package cpath.webservice.args;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

public class GetProperty {
	@NotNull(message="Property Path is not Specified!") 
	@NotBlank(message="Property Path is blank!")
	private String path;
	// required at least one value
	@NotNull(message="Parameter 'uri' is required!")
	@NotEmpty(message="At least one URI has to be set!")
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
