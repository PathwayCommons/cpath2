/**
 * 
 */
package cpath.config;

import static cpath.config.CPathSettings.*;


/**
 * CPath2 Instance bean.
 * 
 * A convenience class to get a snapshot of cpath2 instance configuration 
 * public view properties (no passwords though), and to
 * display, e.g., on the web pages.
 * 
 * @author rodche
 *
 */
public final class CPath {
	
	private String name;
	private String description;
	private String logoUrl;
	private String url;
	private String[] organisms;
	private String version;
	private boolean adminEnabled;
	private String mainDb;
	private String maxHitsPerPage;
	private String xmlBase;
	
	private CPath() {}
	
	
	/**
	 * Returns current cpath2 instance properties.
	 * It's not unmodifiable, but any field change will 
	 * have no effect on the actual properties.
	 * If one (admin) wants to modify the instance properties,
	 * can use {@link CPathSettings#setCPathProperty(String, String)}
	 * and {@link CPathSettings#saveCPathProperties()} methods.
	 * 
	 * @return
	 */
	public static CPath build() {
		CPath cpath = new CPath();
		
		cpath.name = property(PROVIDER_NAME);
		cpath.description = property(PROVIDER_DESCRIPTION);
		cpath.logoUrl = property(PROVIDER_LOGO_URL);
		cpath.url = property(PROVIDER_URL);
		cpath.organisms = organisms();
		cpath.version = property(PROVIDER_VERSION);
		cpath.adminEnabled = isMaintenanceEnabled();
		cpath.mainDb = property(PROP_MAIN_DB);
		cpath.maxHitsPerPage = property(PROP_MAX_SEARCH_HITS_PER_PAGE);
		cpath.xmlBase = property(PROP_XML_BASE);
		
		return cpath;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public String getDescription() {
		return description;
	}


	public void setDescription(String description) {
		this.description = description;
	}


	public String getLogoUrl() {
		return logoUrl;
	}


	public void setLogoUrl(String logoUrl) {
		this.logoUrl = logoUrl;
	}


	public String getUrl() {
		return url;
	}


	public void setUrl(String url) {
		this.url = url;
	}


	public String[] getOrganisms() {
		return organisms;
	}


	public void setOrganisms(String[] organisms) {
		this.organisms = organisms;
	}


	public String getVersion() {
		return version;
	}


	public void setVersion(String version) {
		this.version = version;
	}


	public boolean isAdminEnabled() {
		return adminEnabled;
	}


	public void setAdminEnabled(boolean adminEnabled) {
		this.adminEnabled = adminEnabled;
	}


	public String getMainDb() {
		return mainDb;
	}


	public void setMainDb(String mainDb) {
		this.mainDb = mainDb;
	}


	public String getMaxHitsPerPage() {
		return maxHitsPerPage;
	}


	public void setMaxHitsPerPage(String maxHitsPerPage) {
		this.maxHitsPerPage = maxHitsPerPage;
	}


	public String getXmlBase() {
		return xmlBase;
	}


	public void setXmlBase(String xmlBase) {
		this.xmlBase = xmlBase;
	}
	
}
