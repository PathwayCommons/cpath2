package cpath.jpa;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.zip.GZIPOutputStream;

import javax.persistence.*;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.biopax.validator.api.ValidatorUtils;
import org.biopax.validator.api.beans.Validation;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.util.Assert;

import cpath.config.CPathSettings;

/**
 * A bio pathway/network data file from some data provider.
 */
@Entity
@DynamicUpdate
@DynamicInsert
@Table(name="content", uniqueConstraints=@UniqueConstraint(columnNames = {"provider", "filename"}))
public final class Content {
	
	private static Logger log = LoggerFactory.getLogger(Content.class);

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	@Column(nullable=false)
    private String filename;

	@Column(nullable=false)
	private String provider;

	
	/**
	 * Default Constructor (for persistence)
	 */
	public Content() {}
	
	
    /**
     * Create a Content domain object (value object).
     * 
     * @param provider must be output provider for normalized data and validation reports
     * @param filename file name base (prefix for the normalized data and validation report file names)
     */
    public Content(Metadata provider, String filename)
    {    	
        Assert.notNull(provider,"provider cannot be null");
    	Assert.notNull(filename, "filename cannot be null");
    	this.provider = provider.getIdentifier();
        this.filename = filename.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

	
	public void saveValidationReport(Validation v) {		
		Writer writer;
		try {
			writer = new OutputStreamWriter(
				 new GZIPOutputStream(new FileOutputStream(validationXmlFile())));
			ValidatorUtils.write(v, writer, null);
			writer.flush();
			writer.close();//important
		} catch (IOException e) {
			log.error("saveValidationReport, failed to save the XML report", e);
		}
		
		// transform to html report and save (frankly, not needed) 
		// (fails if old saxon libs present in the classpath, e.g., those come with the psimi-converter...)
		try {
			writer = new OutputStreamWriter(
					 new GZIPOutputStream(new FileOutputStream(validationHtmlFile())));
			StreamSource xsl = new StreamSource((new DefaultResourceLoader())
				.getResource("classpath:html-result.xsl").getInputStream());
			ValidatorUtils.write(v, writer, xsl); 
			writer.flush();
			writer.close();//important
		} catch (Exception e) {
			log.error("saveValidationReport, failed to transform the XML to HTML report", e);
		}		
	}

	@Override
    public String toString() {
		return provider + "/" + filename;
    }


    public String normalizedFile() {
    	return CPathSettings.getInstance().dataDir() + 
    		File.separator + provider 
    		+ File.separator + filename 
    			+ ".normalized.owl.gz";
    }

    
    public String validationXmlFile() {
		return CPathSettings.getInstance().dataDir() + 
			File.separator + provider 
		    + File.separator + filename 
		    	+ ".validation.xml.gz";
    }
    
 
    public String validationHtmlFile() {
		return CPathSettings.getInstance().dataDir() + 
			File.separator +provider 
		    + File.separator + filename 
		    	+ ".validation.html.gz";
    }
    

    public String convertedFile() {
		return CPathSettings.getInstance().dataDir() + 
			File.separator + provider 
		    + File.separator + filename 
		    	+ ".converted.owl.gz";
    }  
    
    public String cleanedFile() {
		return CPathSettings.getInstance().dataDir() + 
			File.separator + provider 
		    + File.separator + filename 
		    	+ ".cleaned.gz";
    }    
    
    public String originalFile() {
		return CPathSettings.getInstance().dataDir() + 
			File.separator + provider 
		    + File.separator + filename 
		    	+ ".original.gz";
    }
    
    
    public String getFilename() {
		return filename;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Content) {
			final Content that = (Content) obj;
			return new EqualsBuilder().append(filename, that.getFilename()).append(provider, that.provider).isEquals();
		}
		else
			return false;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(filename).append(provider).toHashCode();
	}
}
