package cpath.warehouse.beans;

// imports
import javax.persistence.*;

/**
 * Data Providers's Pathway Data.
 */
@Entity
@Table(name="pathwayData")
@NamedQueries({
	@NamedQuery(name="cpath.warehouse.beans.pathwayByIdentifier",
				query="from PathwayData as pathwaydata where upper(pathwaydata.identifier) = upper(:identifier)"),
	@NamedQuery(name="cpath.warehouse.beans.pathwayByIdentifierAndVersion",
				query="from PathwayData as pathwaydata where upper(pathwaydata.identifier) = upper(:identifier) and upper(pathwaydata.version) = upper(:version)"),
	@NamedQuery(name="cpath.warehouse.beans.pathwayByIdentifierAndVersionAndFilenameAndDigest",
				query="from PathwayData as pathwaydata where upper(pathwaydata.identifier) = upper(:identifier) and upper(pathwaydata.version) = upper(:version) and upper(pathwaydata.filename) = upper(:filename) and upper(pathwaydata.digest) = upper(:digest)")
})
public class PathwayData {

	@Id
	@Column(name="pathway_id")
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Integer id;
	@Column(nullable=false)
    private String identifier;
	@Column(nullable=false)
    private String version;
	@Column(nullable=false, unique=true)
    private String filename;
	@Lob
	@Column(nullable=false)
    private String pathwayData;
	@Lob
    private String premergeData;
	@Lob
	private String validationResults;
	// digest is not unique - at least some reactome pw have different names but are identical
	@Column(nullable=false)
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
    public PathwayData(final String identifier, final String version, final String filename, final String digest, final String pathwayData) {

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
        this.filename = filename;

        if (digest == null) {
            throw new IllegalArgumentException("digest must not be null");
        }
        this.digest = digest;

        if (pathwayData == null) {
            throw new IllegalArgumentException("pathway data must not be null");
        }
        this.pathwayData = pathwayData;

		// validation results is empty by default
		this.validationResults = "";
		
		this.premergeData = "";
    }

	public void setId(Integer id) {
		this.id = id;
	}
    public Integer getId() { return id; }

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
    public String getIdentifier() { return identifier; }

	public void setVersion(String version) {
		this.version = version;
	}
    public String getVersion() { return version; }

	public void setFilename(String filename) {
		this.filename = filename;
	}
    public String getFilename() { return filename; }

	public void setPathwayData(String pathwayData) {
		this.pathwayData = pathwayData;
	}
    public String getPathwayData() { return pathwayData; }

	public void setPremergeData(String premergeData) {
		this.premergeData = premergeData;
	}
    public String getPremergeData() { return premergeData; }
    
	public void setValidationResults(String validationResults) {
		this.validationResults = validationResults;
	}
    public String getValidationResults() { return validationResults; }

	public void setDigest(String digest) {
		this.digest = digest;
	}
    public String getDigest() { return digest; }

    @Override
    public String toString() {
        return getIdentifier() + ", " + getVersion() + ", " + getFilename();
    }
}
