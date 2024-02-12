package cpath.service.metadata;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * A spring-data repository (auto-instantiated) of Datasource entities
 * (all methods here follow the spring-data naming and signature conventions,
 * and therefore do not require to be implemented by us; these will be auto-generated).
 * 
 * @author rodche
 */
@Data //getter/setters will be auto-generated at compile time by Lombok, etc.
@NoArgsConstructor
@AllArgsConstructor
public class Metadata {
	private String description;
	private int version;
	private List<Datasource> datasources;

	public Datasource findByIdentifier(String identifier) {
		if(datasources == null || datasources.isEmpty()) {
			return null;
		}
		return datasources.stream().filter(d -> d.getIdentifier().equalsIgnoreCase(identifier))
			.findFirst().orElse(null);
	}
}
