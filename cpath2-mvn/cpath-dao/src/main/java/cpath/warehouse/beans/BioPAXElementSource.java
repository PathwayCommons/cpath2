package cpath.warehouse.beans;

// imports
import javax.persistence.*;

/**
 *  This bean/table is how we associate a BioPAX Entity (via RDFId)
 *  with a data source provider and species.  The creation of this 
 *  bean/table was motivated by the need to provide filtering by 
 *  datasource / organism at the web service level.
 */
@Entity
@Table(name="BioPAXElementSource")
@NamedQueries({
	@NamedQuery(name="cpath.warehouse.beans.biopaxElementSourceByRDFIdentifier",
				query="from BioPAXElementSource as bpe where bpe.rdfId = :rdfId"),
	@NamedQuery(name="cpath.warehouse.beans.biopaxElementSourceByTaxIdentifier",
				query="from BioPAXElementSource as bpe where bpe.taxId = :taxId"),
	@NamedQuery(name="cpath.warehouse.beans.biopaxElementSourceByProviderIdentifier",
				query="from BioPAXElementSource as bpe where bpe.providerId = :providerId")
})
public class BioPAXElementSource {
	
	@Id
	@Column(length=333,nullable=false)
	private String rdfId;
	@Column(nullable=false)
	private String taxId;
	@Column(nullable=false)
	private String providerId; // not Metadata.provider_id, but Metadata.identifier
	
	/**
	 * Default Constructor.
	 */
	public BioPAXElementSource() {}

	/**
	 * Create a BioPAXElementSource obj with the specified properties;
	 * 
	 * @param rdfId String (rdfID of BioPAXElement)
	 * @param taxId String (source organism for this BioPAXElement)
	 * @param providerId String (not Metadata.provider_id, but Metadata.identifier)
	 */
	public BioPAXElementSource(final String rdfId, final String taxId, final String providerId) {
		
		if (rdfId == null) {
			throw new IllegalArgumentException("rdfId must not be null");
		}
		
		if (taxId == null) {
			throw new IllegalArgumentException("taxId must not be null");
		}
		
		if (providerId == null) {
			throw new IllegalArgumentException("providerId must not be null");
		}
	}
	
	public void setRDFId(String rdfId) {
		this.rdfId = rdfId;
	}
	public String getRDFId() { return rdfId; }
	
	public void setTaxId(String taxId) {
		this.taxId = taxId;
	}
	public String getTaxId() { return taxId; }
	
	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}
	public String getProviderId() { return providerId; }
}
