package cpath.webservice;

public enum GraphType {
	NEIGHBORHOOD("neighborhood"),
	COMMON_UPSTREAM("common upstream"),
	COMMON_DOWNSTREAM("common downstream"),
	COMMON_TARGET("common target"),
	NETWORK_OF_INTEREST("network of interest"),
	K_SHORTEST_PATH("k-shortest path"),
	;
	
	private String value;

	private GraphType(String value) {
		this.value = value;
	}
	
	@Override
	public String toString() {
		return value;
	}
	
	public static GraphType parseGraphType(String value) {
		for(GraphType v : GraphType.values()) {
			if(value.equalsIgnoreCase(v.toString())) {
				return v;
			}
		}
		return null;
	}
}
