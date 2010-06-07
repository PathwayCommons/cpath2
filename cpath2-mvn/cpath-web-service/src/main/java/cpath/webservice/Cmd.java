package cpath.webservice;

/**
 * Valid cPath web service commands; 
 * added for backward compatibility.
 * 
 * @author rodche
 *
 */
public enum Cmd {
	SEARCH,
	GET_PATHWAYS,
	GET_NEIGHBORS,
	GET_PARENTS,
	GET_RECORD_BY_CPATH_ID;
	
	public static Cmd parseCmd(String value) {
		for(Cmd v : Cmd.values()) {
			if(value.equalsIgnoreCase(v.toString())) {
				return v;
			}
		}
		return null;
	}
}
