package cpath.warehouse.beans;

// imports
import java.io.File;
import java.net.URI;
import java.util.regex.Pattern;

import javax.persistence.*;

import cpath.config.CPathSettings;

/**
 * Data Provider Metadata.
 */
@Entity
@Table(name="metadata")
@NamedQueries({
		@NamedQuery(name="cpath.warehouse.beans.providerByIdentifier",
					query="from Metadata as metadata where identifier = :identifier order by identifier"),
		@NamedQuery(name="cpath.warehouse.beans.allProvider", 
					query="from Metadata as metadata order by identifier")
})
public final class Metadata {

    private static final Pattern BAD_ID_PATTERN = Pattern.compile("\\s|-");
	
	// TYPE Enum
    public static enum TYPE {
        // command types
        PSI_MI(false),
		BIOPAX(false),
		PROTEIN(true),
		SMALL_MOLECULE(true),
		MAPPING(true);
        
        private final boolean warehouseData;
        
        private TYPE(boolean warehouseData) {
			this.warehouseData = warehouseData;
		}
        
        public boolean isWarehouseData() {
			return warehouseData;
		}
        
    }

	@Id
	@Column(length=40)
    private String identifier;
	@Column(nullable=false)
    private String name;
	@Column(nullable=false)
    private String version;
	@Column(nullable=false)
    private String description;
	@Column(nullable=false)
    private String urlToData;
	@Lob
	@Column(nullable=false)
    private byte[] icon;
	@Column(nullable=false)
	@Enumerated(EnumType.STRING)
    private TYPE type;
    private String cleanerClassname;
    private String converterClassname;

	/**
	 * Default Constructor.
	 */
	public Metadata() {}

    /**
     * Create a Metadata obj with the specified properties;
     *
     * @param identifier String (string used in web service calls)
     * @param name String
     * @param version Float
     * @param description String
     * @param urlToData String
     * @param icon byte[]
     * @param isPSI Boolean
	 * @param cleanerClassname String
	 * @param converterClassname String
	 * 
	 * @throws IllegalArgumentException
     */
    public Metadata(final String identifier, final String name, final String version, final String description, final String urlToData,
					final byte[] icon, final TYPE type, final String cleanerClassname, final String converterClassname) {

    	//set/validate all parameters
    	setIdentifier(identifier); 
        setName(name);
        setVersion(version);
        setDescription(description);
        setURLToData(urlToData);
        setIcon(icon);
        setType(type);
        setCleanerClassname(cleanerClassname);
        setConverterClassname(converterClassname);
    }

	/**
	 * Sets the identifier.
	 * No spaces, dashes, allowed. 
	 * 
	 * @param identifier
	 * @throws IllegalArgumentException if it's null, empty string, or contains spaces or dashes
	 */
    void setIdentifier(String identifier) {
    	// validate the parameter
    	
    	if(identifier == null 
    		|| identifier.length() == 0
    		|| BAD_ID_PATTERN.matcher(identifier).find())
    		throw new IllegalAccessError("Bad metadata identifier: " + identifier);
    		
		// copy value
    	this.identifier = identifier;
	}
	
	/**
	 * Data source metadata identifier.
	 * 
	 * It can be also used as filter ('datasource') 
	 * value in cpath2 full-text search queries
	 * (for pathway datasource types only)
	 * 
	 * @return
	 */
    public String getIdentifier() { return identifier; }

	/**
	 * Sets data provider/source name. 
	 * 
	 * Please use a standard name for pathway/interaction data types,
	 * if possible (for warehouse data it's not so important), 
	 * as this will be recommended to use as filter ('datasource') 
	 * value in cpath2 full-text search queries 
	 * 
     * @param name
     * @throws IllegalArgumentException
     */
	public void setName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
		this.name = name;
	}
	
	/**
	 * Gets the data provider/source name.
	 * 
	 * @return
	 */
    public String getName() { return name; }

	public void setVersion(String version) {
        if (version == null) {
            throw new IllegalArgumentException("version must not be null");
        }
        this.version = version;
	}
    public String getVersion() { return version; }

	public void setDescription(String releaseDate) {
        if (releaseDate == null) {
            throw new IllegalArgumentException("release data must not be null");
        }
        this.description = releaseDate;
	}
    public String getDescription() { return description; }

	public void setURLToData(String urlToData) {
        if (urlToData == null) {
            throw new IllegalArgumentException("URL to data must not be null");
        }
        this.urlToData = urlToData;
	}
    public String getURLToData() { return urlToData; }

	public void setIcon(byte[] icon) {
        if (icon == null) {
            throw new IllegalArgumentException("icon must not be null");
        }
        this.icon = icon;
	}
    public byte[] getIcon() { return icon.clone(); }

	public void setType(TYPE type) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        this.type = type;
	}
    public TYPE getType() { return type; }

	public void setCleanerClassname(String cleanerClassname) {
		if(cleanerClassname == null || cleanerClassname.trim().length() == 0)
			this.cleanerClassname = null;
		else 
			this.cleanerClassname = cleanerClassname.trim();
	}
    public String getCleanerClassname() { 
    	return (cleanerClassname == null || cleanerClassname.length() == 0) 
    			? null : cleanerClassname; 
    }

	public void setConverterClassname(String converterClassname) {
		if(converterClassname == null || converterClassname.trim().length() == 0)
			this.converterClassname = null;
		else
			this.converterClassname = converterClassname.trim();
	}
    public String getConverterClassname() { 
    	return (converterClassname == null || converterClassname.length() == 0) 
    			? null : converterClassname; 
    }

    @Override
    public String toString() {
        return identifier + "." + version;
    }
    
    
	/**
	 * Gets the full local directory path 
	 * where this type of data will be
	 * temporarily fetched, stored,
	 * re-used.
	 * 
	 * @return
	 */
    @Transient
	public String localDataDir() {
		return CPathSettings.getHomeDir() 
			+ File.separator + CPathSettings.DATA_SUBDIR_NAME;
	}
    
	/**
	 * Gets the full path to the local data file
	 * 
	 * @return
	 */
    @Transient
    public String localDataFile() {
    	String name = localDataDir() 
    	+ File.separator + identifier + "." + version;
    	
    	// add the file extension, if any, if different from the version...
		int idx = urlToData.lastIndexOf('.');
		if(idx >= 0) {
			String ext = urlToData.substring(idx+1);
			if(!version.equals(ext))
				name += "." + ext;
		}
		
		return name;
    }
    
    
    /**
     * Generate a URI (for a Provenance instance.)
     * 
     * @return
     * 
     * TODO maybe add/use 'uri' property and constructor arg to set the URI from the conf. file instead. 
     */
    @Transient
    public String uri() {
    	return URI.create("urn:biopax:Provenance:" + identifier + "_" + version)
    			.toString();
//    	return CPathSettings.generateInstanceSpecificURI(getName().toLowerCase(), Provenance.class);
    }
}
