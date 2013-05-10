package cpath.warehouse.beans;


import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import javax.persistence.*;
import javax.xml.transform.stream.StreamSource;

import org.biopax.validator.api.ValidatorUtils;
import org.biopax.validator.api.beans.Validation;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.util.Assert;

import cpath.dao.CPathUtils;

/**
 * Data Providers's Pathway Data.
 * 
 */
@Entity
@Table(name="data", uniqueConstraints=@UniqueConstraint(columnNames = {"directory", "filename"}))
public final class PathwayData {

	@Id
	@Column(name="pathway_id")
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Integer id;
	
	@Column(nullable=false)
    private String filename;
	
	@Transient
    private byte[] data;
	
	@Transient
    private byte[] normalizedData;
	
	@Transient
	private byte[] validationReport;	
	
	@Column
	private Boolean valid; //BioPAX validation status.

	@Column
	private String directory;

	
	/**
	 * Default Constructor (for persistence)
	 */
	public PathwayData() {}
	
	
    /**
     * Create a PathwayData domain object (value object).
     * 
     * @param directory must be output directory for normalized data and validation reports
     * @param filename file name base (prefix for the normalized data and validation report file names)
     */
    public PathwayData(String directory, String filename) 
    {    	
        Assert.notNull(directory);
    	Assert.notNull(filename);
    	this.directory = directory;
        this.filename = filename.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

	
	/**
	 * Gets the internal id (primary key).
	 * 
	 * @return
	 */
    public Integer getId() { return id ;}

    
    
    public byte[] getData() { 
    	if(data == null)
    		data = CPathUtils.zread(convertedFile());
   		return data;
    }

    
    public void setData(byte[] bytes) {
        this.data = bytes;
        CPathUtils.zwrite(convertedFile(), bytes);
	}

	
	public byte[] getNormalizedData() {
		if(normalizedData == null)
			//read file
			normalizedData = CPathUtils.zread(normalizedFile());
		
   		return normalizedData; 
    }

	
	public void setNormalizedData(byte[] bytes) {
		this.normalizedData = bytes;	
		// save BioPAX RDF/XML
		CPathUtils.zwrite(normalizedFile(), bytes);
	}

	
    public byte[] getValidationReport() { 
    	if(validationReport == null)
    		//read file 
    		validationReport = CPathUtils.zread(validationXmlFile());
    	
    	return validationReport; 
    }

    
	public void setValidationReport(Validation v) {
		StringWriter writer = new StringWriter();		
		ValidatorUtils.write(v, writer, null);
		writer.flush();	
		
		this.validationReport = writer.toString().getBytes();
		// save xml to file
		CPathUtils.zwrite(validationXmlFile(), validationReport);	
		
		StreamSource xsl;
		try {
			xsl = new StreamSource((new DefaultResourceLoader())
					.getResource("classpath:html-result.xsl").getInputStream());
		} catch (IOException e) {
			throw new RuntimeException("setValidationResults: failed opening xsl", e);
		}
		writer = new StringWriter();
		ValidatorUtils.write(v, writer, xsl); 
		writer.flush();
		
		// save html to file
		CPathUtils.zwrite(validationHtmlFile(), writer.toString().getBytes());	
	}

	/**
	 * Gets BioPAX validation status.
	 * @return
	 */
    public Boolean getValid() {
		return valid;
	}

    
    public void setValid(Boolean valid) {
		this.valid = valid;
	}

	
	@Override
    public String toString() {
		return directory + "/" + filename + " (" + status() + ")";
    }


    public String normalizedFile() {
    	return directory 
    		+ File.separator + filename 
    			+ ".normalized.owl.gz";
    }

    
    public String validationXmlFile() {
		return directory 
		    + File.separator + filename 
		    	+ ".validation.xml.gz";
    }
    
 
    public String validationHtmlFile() {
		return directory 
		    + File.separator + filename 
		    	+ ".validation.html.gz";
    }
    

    public String convertedFile() {
		return directory 
		    + File.separator + filename 
		    	+ ".converted.owl.gz";
    }     
    
    
    public String getFilename() {
		return filename;
	}
    
    
    public String status() {
    	String s = "not checked";
    	if(valid == Boolean.TRUE)
    		s = "no errors";
    	else if(valid == Boolean.FALSE)
    		s = "has errors";
    	return s;
    }
}
