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
 * unambiguously associate a chemical identifier
 * (PubChem, InChIKey, secondary ChEBI ids)
 * with the corresponding primary ChEBI ID.
 * 
 * This does not store any information about the 
 * type of mapped identifiers: it does not matter,
 * because we suppose non-numeric gene identifiers 
 * are unique across databases, and the only numerical
 * one here will be PubChem substance id.
 * 
 * @author rodche
 */
@Entity
@Table(name="chemidmapping")
@NamedQueries({
		@NamedQuery(name="cpath.warehouse.beans.inverseChemMapping",
			query="from ChemMapping where accession = :accession order by identifier"),
		@NamedQuery(name="cpath.warehouse.beans.allChemMapping",
			query="from ChemMapping")
})
public final class ChemMapping implements IdMapping {

	@Id
	@Column(length=25) //InChiKey has max length 25 chars
    private String identifier;
	
	@Column(length=15) // should be ok for all ChEBI IDs
    private String accession;

	/**
	 * Default Constructor.
	 */
	public ChemMapping() {}

 
	/**
	 * Constructor with parameters.
	 * 
	 * @param identifier
	 * @param accession not null (ChEBI AC)
	 * @throws AssertException when an identifier or accession is null/empty or contains spaces
	 */
    public ChemMapping(final String identifier, final String accession) {
    	if(accession == null || accession.matches("\\s+"))
    		throw new AssertException("Bad accession number");
    	
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
