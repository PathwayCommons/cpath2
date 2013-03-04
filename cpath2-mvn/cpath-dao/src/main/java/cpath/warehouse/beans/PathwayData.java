package cpath.warehouse.beans;


import javax.persistence.*;

import org.hibernate.annotations.ColumnTransformer;

/**
 * Data Providers's Pathway Data.
 * 
 * Note: unfortunately, it's database-specific...
 * 
 */
@Entity
@Table(name="pathwayData")
@NamedQueries({
	@NamedQuery(name="cpath.warehouse.beans.allPathwayData",
				query="from PathwayData as pathwaydata order by pathway_id"),
	@NamedQuery(name="cpath.warehouse.beans.pathwayByIdentifier",
				query="from PathwayData as pathwaydata where identifier = :identifier order by pathway_id"),
	@NamedQuery(name="cpath.warehouse.beans.pathwayByIdentifierAndFilenameAndDigest",
				query="from PathwayData as pathwaydata where identifier = :identifier and filename = :filename and digest = :digest  order by pathway_id")
})
public final class PathwayData {

	@Id
	@Column(name="pathway_id")
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Integer id;
	@Column(nullable=false, length=40)
    private String identifier;
	@Column(nullable=false)
    private String filename;
	
//	@Column(nullable=false)
	@Column(name="pathwayData", columnDefinition = "LONGBLOB", nullable = false)
	// function UNCOMPRESS works in MySQL, but it's called EXPAND in H2...
	@ColumnTransformer(forColumn="pathwayData", read = "UNCOMPRESS(pathwayData)", write = "COMPRESS(?)")
	@Lob
    private byte[] pathwayData;
	
	@Column(name="premergeData", columnDefinition = "LONGBLOB")
	@ColumnTransformer(forColumn="premergeData", read = "UNCOMPRESS(premergeData)", write = "COMPRESS(?)")
	@Lob	
    private byte[] premergeData;
	
	@Column(name="validationResults", columnDefinition = "LONGBLOB")
	@ColumnTransformer(forColumn="validationResults", read = "UNCOMPRESS(validationResults)", write = "COMPRESS(?)")
	@Lob	
	private byte[] validationResults;
	
	// digest is not unique - at least some reactome pw have different names but are identical
	@Column(nullable=false)
    private String digest;
	@Column
	private Boolean valid;

	/**
	 * Default Constructor.
	 */
	public PathwayData() {}

    /**
     * Create a Metadata obj with the specified properties;
     *
     * @param identifier String (string used in web service calls)
     * @param filename String
     * @param digest String
     * @param pathwayData String
	 * @throws IllegalArgumentException
     */
    public PathwayData(final String identifier, final String filename, final String digest, 
    		final byte[] pathwayData) 
    {
    	setIdentifier(identifier);
    	setFilename(filename);
    	setDigest(digest);
    	setPathwayData(pathwayData);
		// validation result, valid, and premergeData fields are empty
    }

    //generated id (not public setter/getter)
	void setId(Integer id) {
		this.id = id;
	}
    public Integer getId() { return new Integer(id); }

	void setIdentifier(String identifier) {
        if (identifier == null) {
            throw new IllegalArgumentException("identifier must not be null");
        }
        this.identifier = identifier;
	}
    public String getIdentifier() { 
    	return (identifier != null) ? new String(identifier) : null;
    }

	void setFilename(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("filename must not be null");
        }
        this.filename = filename;
	}
    public String getFilename() { return filename; }

    public byte[] getPathwayData() { 
   		return pathwayData;
    }
	public void setPathwayData(byte[] pathwayData) {
        if (pathwayData == null || pathwayData.length == 0) {
            throw new IllegalArgumentException("pathway data must not be null/empty");
        }
        this.pathwayData = pathwayData;
	}

	public byte[] getPremergeData() { 
   		return premergeData; 
    }
	public void setPremergeData(byte[] premergeData) {
		this.premergeData = premergeData;	
	}
	
    public byte[] getValidationResults() { 
    	return validationResults; 
    }
	public void setValidationResults(byte[] validationResults) {
		this.validationResults = validationResults;
	}

	public void setDigest(String digest) {
        if (digest == null) {
            throw new IllegalArgumentException("digest must not be null");
        }
        this.digest = digest;
	}
    public String getDigest() { 
    	return (digest != null) ? new String(digest) : null; }

    public Boolean getValid() {
		return valid;
	}
	public void setValid(Boolean valid) {
		this.valid = valid;
	}

	@Override
    public String toString() {
        return getId() + ": " + getIdentifier() + ", " + getFilename();
    }

}
