package cpath.warehouse.beans;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import com.mchange.util.AssertException;

import cpath.dao.IdMapping;

/**
 * A simple id-mapping db table to
 * unambiguously associate a gene/protein identifier
 * with the corresponding primary uniprot accession 
 * 
 * This does not store any information about the 
 * type of mapped identifiers: it does not matter,
 * because we suppose non-numeric gene identifiers 
 * are unique across databases, and the only numerical
 * one here will be NCBI Gene (Entrez Gene) id.
 * 
 * @author rodche
 */
@Entity
@Table(name="geneidmapping")
@NamedQueries({
		@NamedQuery(name="cpath.warehouse.beans.inverseGeneMapping",
			query="from GeneMapping where accession = :accession order by identifier"),
		@NamedQuery(name="cpath.warehouse.beans.allGeneMapping",
			query="from GeneMapping")
})
public final class GeneMapping implements IdMapping {

	@Id
	@Column(length=15) //Ensemble has the longest ids
    private String identifier;
	
	@Column(columnDefinition="CHAR(6)")
    private String accession;

	/**
	 * Default Constructor.
	 */
	public GeneMapping() {}

 
	/**
	 * Constructor with parameters.
	 * 
	 * @param identifier
	 * @param accession not null (UniProt or ChEBI AC)
	 * @throws AssertException when an identifier or accession is null/empty, contains spaces, or accession is not 6-chars long
	 */
    public GeneMapping(final String identifier, final String accession) {
    	if(accession == null || accession.length() != 6 || accession.matches("\\s+"))
    		throw new AssertException("Bad UniProt accession number");
    	
    	if(identifier == null || identifier.isEmpty())
    		throw new AssertException("Bad identifier");
    	
    	this.identifier = identifier;
    	this.accession = accession;
    }

    //private setter
    void setIdentifier(String identifier) {    	
    	this.identifier = identifier;
	}
    public String getIdentifier() { 
    	return identifier;
    }
  
    //private setter
    void setAccession(String accession) {
		this.accession = accession;
	}   
    public String getAccession() {
		return accession;
	}

}
