package cpath.warehouse.beans;


import javax.persistence.*;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

/**
 * Data Providers's Pathway Data.
 * 
 */
@Entity
@DynamicUpdate
@DynamicInsert
@Table(name="pathwayData", uniqueConstraints=@UniqueConstraint(columnNames = {"metadata_id", "filename"}))
@NamedQueries({
	@NamedQuery(name="cpath.warehouse.beans.allPathwayData",
				query="from PathwayData as pathwaydata order by pathway_id"),
	@NamedQuery(name="cpath.warehouse.beans.pathwayDataByIdentifier",
				query="from PathwayData as pd where pd.metadata.identifier = :identifier order by pathway_id"),
	@NamedQuery(name="cpath.warehouse.beans.uniquePathwayData",
				query="from PathwayData as pd where pd.metadata.identifier = :identifier and filename = :filemane")
})
public final class PathwayData {

	@Id
	@Column(name="pathway_id")
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Integer id;
	
	@ManyToOne(optional=false)
	@JoinColumn(name="metadata_id")
    private Metadata metadata;
	
	@Column(nullable=false)
    private String filename;
	
	@Transient
    private byte[] pathwayData;
	@Transient
    private byte[] premergeData;
	@Transient
	private byte[] validationResults;
	
	@Column
	private Boolean valid;

	/**
	 * Default Constructor.
	 */
	public PathwayData() {}

    /**
     * Create a Metadata obj with the specified properties;
     *
     * @param metadata the provider's metadata object it belongs to
     * @param filename String
     * @param pathwayData String
	 * @throws IllegalArgumentException
     */
    public PathwayData(Metadata metadata, final String filename, final byte[] pathwayData) 
    {
    	this.metadata = metadata;
    	setFilename(filename);
    	setPathwayData(pathwayData);
		// validation result, valid, and premergeData fields are empty
    }

    //generated id (not public setter/getter)
	void setId(Integer id) {
		this.id = id;
	}
	
	/**
	 * Gets the internal id (primary key) 
	 * of this pathway data (file) entry.
	 * 
	 * This is made public to be used in 
	 * web pages/queries about individual files 
	 * validation results, etc. 
	 * 
	 * @return
	 */
    public Integer getId() { return id ;}

    public Metadata getMetadata() {
		return metadata;
	}
    public void setMetadata(Metadata metadtaa) {
		this.metadata = metadtaa;
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

    public Boolean getValid() {
		return valid;
	}
	public void setValid(Boolean valid) {
		this.valid = valid;
	}

	@Override
    public String toString() {
        return "PathwayData " + getId() + " source " + getIdentifier() 
        	+ ((filename != null && filename.length()>4) ? " file " + filename : "");
    }

	/**
	 * Gets the parent metadata's 
	 * (data source's) identifier.
	 * 
	 * @return
	 */
	@Transient
	public String getIdentifier() {
		return (metadata != null) ? metadata.getIdentifier() : null;
	}

}
