package cpath.warehouse.beans;


import java.io.File;

import javax.persistence.*;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import cpath.config.CPathSettings;
import cpath.dao.CPathUtils;

/**
 * Data Providers's Pathway Data.
 * 
 */
@Entity
@DynamicUpdate
@DynamicInsert
@Table(name="pathwayData", uniqueConstraints=@UniqueConstraint(columnNames = {"metadata_id", "filename"}))
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
    public PathwayData(Metadata metadata, final String filename) 
    {
    	this.metadata = metadata;
    	setFilename(filename);
    }


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
        this.pathwayData = pathwayData;
	}

	
	public byte[] getPremergeData() {
		if(premergeData == null)
			//read file
			premergeData = CPathUtils.zread(premergedFile());
		
   		return premergeData; 
    }

	
	public void setPremergeData(byte[] premergeData) {
		this.premergeData = premergeData;	
		// save BioPAX RDF/XML
		CPathUtils.zwrite(premergedFile(), premergeData);
	}

	
    public byte[] getValidationResults() { 
    	if(validationResults == null)
    		//read file 
    		validationResults = CPathUtils.zread(validationFile());
    	
    	return validationResults; 
    }

    
	public void setValidationResults(byte[] validationResults) {
		this.validationResults = validationResults;
		// save file
		CPathUtils.zwrite(validationFile(), validationResults);		
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


    public String premergedFile() {
    	return CPathSettings.dataDir() 
    	+ File.separator + getIdentifier() 
    	+ File.separator + filename + ".rdf.gz";
    }

    
    public String validationFile() {
		return premergedFile() + ".validation.xml.gz";
    }

}
