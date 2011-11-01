package cpath.warehouse.beans;

// imports
import java.io.File;

import javax.persistence.*;

/**
 * Data Provider Metadata.
 */
@Entity
@Table(name="metadata")
@NamedQueries({
		@NamedQuery(name="cpath.warehouse.beans.providerByIdentifier",
					query="from Metadata as metadata where upper(metadata.identifier) = upper(:identifier)"),
		@NamedQuery(name="cpath.warehouse.beans.allProvider", 
					query="from Metadata as metadata")
})
public class Metadata {

    // TYPE Enum
    public static enum TYPE {
        // command types
        PSI_MI,
		BIOPAX,
		PROTEIN,
		SMALL_MOLECULE,
		MAPPING;
    }

	@Id
	@Column(name="provider_id")
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Integer id;
	@Column(nullable=false, unique=true)
    private String identifier;
	@Column(nullable=false)
    private String name; // MUST be a Miriam standard name/synonym for the datasource!!!
	@Column(nullable=false)
    private String version;
	@Column(nullable=false)
    private String releaseDate;
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
     * @param releaseDate String
     * @param urlToData String
     * @param icon byte[]
     * @param isPSI Boolean
	 * @param cleanerClassname String
	 * @param converterClassname String
     */
    public Metadata(final String identifier, final String name, final String version, final String releaseDate, final String urlToData,
					final byte[] icon, final TYPE type, final String cleanerClassname, final String converterClassname) {

        if (identifier == null) {
            throw new IllegalArgumentException("identifier must not be null");
        }
        
        // MySQL can't handle dashes in database names (metadata identifier may become part of db name...)
        if(identifier.contains("-")) {
        	this.identifier = identifier.replaceAll("-", "_");
        } else {
        	this.identifier = identifier;
        }
        
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        this.name = name;

        if (version == null) {
            throw new IllegalArgumentException("version must not be null");
        }
        this.version = version;

        if (releaseDate == null) {
            throw new IllegalArgumentException("release data must not be null");
        }
        this.releaseDate = releaseDate;

        if (urlToData == null) {
            throw new IllegalArgumentException("URL to data must not be null");
        }
        this.urlToData = urlToData;

        if (icon == null) {
            throw new IllegalArgumentException("icon must not be null");
        }
        this.icon = icon;

        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        this.type = type;

		if (cleanerClassname == null) {
			throw new IllegalArgumentException("cleaner class name cannot be null");
		}
		this.cleanerClassname = cleanerClassname;

		
		if (converterClassname == null) {
			throw new IllegalArgumentException("converter class name cannot be null");
		}
		this.converterClassname = converterClassname;
    }

	public void setId(Integer id) {
		this.id = id;
	}
    public Integer getId() { return id; }

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
    public String getIdentifier() { return identifier; }

	public void setName(String name) {
		this.name = name;
	}
    public String getName() { return name; }

	public void setVersion(String version) {
		this.version = version;
	}
    public String getVersion() { return version; }

	public void setReleaseDate(String releaseData) {
		this.releaseDate = releaseDate;
	}
    public String getReleaseDate() { return releaseDate; }

	public void setURLToData(String urlToData) {
		this.urlToData = urlToData;
	}
    public String getURLToData() { return urlToData; }

	public void setIcon(byte[] icon) {
		this.icon = icon;
	}
    public byte[] getIcon() { return icon; }

	public void setType(TYPE type) {
		this.type = type;
	}
    public TYPE getType() { return type; }

	public void setCleanerClassname(String cleanerClassname) {
		this.cleanerClassname = cleanerClassname;
	}
    public String getCleanerClassname() { return cleanerClassname; }

	public void setConverterClassname(String converterClassname) {
		this.converterClassname = converterClassname;
	}
    public String getConverterClassname() { return converterClassname; }

    @Override
    public String toString() {
        return identifier + "." + version;
    }
    
    
	/**
	 * Gets the full local directory name 
	 * (within CPATH2_HOME directory)
	 * where this type of data are/will be
	 * stored.
	 * 
	 * @return
	 */
    @Transient
	public String getDataLocalDir() {
		//return CPathSettings.getHomeDir() 
		//	+ File.separator + type.name().toLowerCase();
    	return System.getProperty("java.io.tmpdir");
	}
    
	/**
	 * Gets the full path to the local data file
	 * 
	 * @return
	 */
    @Transient
    public String getLocalDataFile() {
    	String name = getDataLocalDir() 
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
}
