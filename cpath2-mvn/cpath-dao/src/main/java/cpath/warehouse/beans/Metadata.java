package cpath.warehouse.beans;

// imports
import javax.persistence.*;
import org.hibernate.search.annotations.Indexed;

import cpath.config.CPathSettings;

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
@Indexed(index=CPathSettings.WHOUSE_SEARCH_INDEX)
public final class Metadata {

    // TYPE Enum
    public static enum TYPE {

        // command types
        PSI_MI("PSI-MI"),
		BIOPAX("BIOPAX"),
		BIOPAX_L2("BIOPAX_L2"),
		PROTEIN("PROTEIN"),
		SMALL_MOLECULE("SMALL-MOLECULE");

        // string ref for readable name
        private String type;
        
        // contructor
        TYPE(String type) { this.type = type; }

        // method to get enum readable name
        public String toString() { return type; }
    }

	@Id
	@Column(name="provider_id")
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Integer id;
	@Column(nullable=false, unique=true)
    private String identifier;
	@Column(nullable=false)
    private String name;
	@Column(nullable=false)
    private Float version;
	@Column(nullable=false)
	private Float persistedVersion; // version that was persisted
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
    public Metadata(final String identifier, final String name, final Float version, final String releaseDate, final String urlToData,
					final byte[] icon, final TYPE type, final String cleanerClassname, final String converterClassname) {

        if (identifier == null) {
            throw new IllegalArgumentException("identifier must not be null");
        }
        this.identifier = identifier;

        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        this.name = name;

        if (version == null) {
            throw new IllegalArgumentException("version must not be null");
        }
        this.version = version;
		this.persistedVersion = 0.0f;

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

	public void setVersion(Float version) {
		this.version = version;
	}
    public Float getVersion() { return version; }

	public void setPersistedVersion(Float persistedVersion) {
		this.persistedVersion = persistedVersion;
	}
    public Float getPersistedVersion() { return persistedVersion; }

	public void setReleaseDate(String releaseData) {
		this.releaseDate = releaseDate;
	}
    public String getReleaseDate() { return releaseDate; }

	public void setURLToPathwayData(String urlToData) {
		this.urlToData = urlToData;
	}
    public String getURLToPathwayData() { return urlToData; }

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
        return identifier;
    }
}