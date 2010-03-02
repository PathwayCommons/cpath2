package cpath.warehouse.beans;

// imports
import javax.persistence.*;
import org.hibernate.Query;

/**
 * Data Provider Metadata.
 */
@Entity
@Table(name="pathway")
@NamedQueries({
	@NamedQuery(name="cpath.warehouse.beans.pathwayByIdentifier",
				query="from PathwayData as pathwaydata where upper(pathwaydata.identifier) = upper(:identifier)"),
	@NamedQuery(name="cpath.warehouse.beans.pathwayByIdentifierAndVersion",
				query="from PathwayData as pathwaydata where upper(pathwaydata.identifier) = upper(:identifier) and pathwaydata.version = :version"),
	@NamedQuery(name="cpath.warehouse.beans.pathwayByIdentifierAndVersionAndDigest",
				query="from PathwayData as pathwaydata where upper(pathwaydata.identifier) = upper(:identifier) and pathwaydata.version = :version and upper(pathwaydata.digest) = upper(:digest)")
})
public final class PathwayData {

	@Id
	@Column(name="pathway_id")
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Integer id;
	@Column(nullable=false)
    private String identifier;
	@Column(nullable=false)
    private Float version;
	@Column(nullable=false, unique=true)
    private String filename;
	@Column(nullable=false)
    private String pathwayData;
	@Column(nullable=false, unique=true)
    private String digest;

	/**
	 * Default Constructor.
	 */
	public PathwayData() {}

    /**
     * Create a Metadata obj with the specified properties;
     *
     * @param identifier String (string used in web service calls)
	 * @param version Float
	 * @param filename String
	 * @param digest String
     * @param pathwayData String
     */
    public PathwayData(final String identifier, final Float version, final String filename, final String digest, final String pathwayData) {

        if (identifier == null) {
            throw new IllegalArgumentException("identifier must not be null");
        }
        this.identifier = identifier;

        if (version == null) {
            throw new IllegalArgumentException("version must not be null");
        }
        this.version = version;

        if (filename == null) {
            throw new IllegalArgumentException("filename must not be null");
        }
        this.digest = digest;

        if (digest == null) {
            throw new IllegalArgumentException("digest must not be null");
        }
        this.digest = digest;

        if (pathwayData == null) {
            throw new IllegalArgumentException("pathway data must not be null");
        }
        this.pathwayData = pathwayData;
    }

	public void setId(Integer id) {
		this.id = id;
	}
    public Integer getId() { return id; }

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
    public String getIdentifier() { return identifier; }

	public void setVersion(Float version) {
		this.version = version;
	}
    public Float getVersion() { return version; }

	public void setFileName(String filename) {
		this.filename = filename;
	}
    public String getFileName() { return filename; }

	public void setPathwayData(String pathwayData) {
		this.pathwayData = pathwayData;
	}
    public String getPathwayData() { return pathwayData; }

	public void setDigest(String digest) {
		this.digest = digest;
	}
    public String getDigest() { return digest; }

    @Override
    public String toString() {
        return identifier;
    }
}
