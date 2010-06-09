package cpath.webservice.args;

public enum CPathProtocolVersion {
	VERSION_1("1.0"), 
	VERSION_2("2.0"), 
	VERSION_3("3.0");
	
	public final String value;
	
	private CPathProtocolVersion(String v) {
		value = v;
	}
	
}
