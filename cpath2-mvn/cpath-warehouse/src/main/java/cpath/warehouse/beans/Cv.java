/**
 ** Copyright (c) 2009 Memorial Sloan-Kettering Cancer Center (MSKCC)
 ** and University of Toronto (UofT).
 **
 ** This is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** any later version.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** both UofT and MSKCC have no obligations to provide maintenance, 
 ** support, updates, enhancements or modifications.  In no event shall
 ** UofT or MSKCC be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** UofT or MSKCC have been advised of the possibility of such damage.  
 ** See the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this software; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA;
 ** or find it at http://www.fsf.org/ or http://www.gnu.org.
 **/

package cpath.warehouse.beans;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

import org.biopax.paxtools.proxy.StringSetBridge;
import org.hibernate.annotations.CollectionOfElements;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;

import cpath.warehouse.CPathWarehouse;

/**
 * Managed by CPathWarehouse BioPAX Controlled Vocabulary
 * (CV's hierarchy is also available)
 * 
 * @author rodche
 *
 */
@Entity(name = "cv")
@Indexed(index=CPathWarehouse.SEARCH_INDEX_NAME)
public class Cv implements Serializable {

	private static final long serialVersionUID = 1L;

	private Integer id = 0;
	private Set<Cv> members;
	private String urn;
	private String type;
	private String ontology;
	private String accession;
	private String name;
	private Set<String> synonyms;
	
	@Id
    @GeneratedValue
    @Column(name="cv_id")
	public Integer getId() {
		return id;
	}

	public void setId(Integer value) {
		id = value;
	}
	
	public Cv() {
		synonyms = new HashSet<String>();
		members = new HashSet<Cv>();
	}

	public Cv(String type, 
			String urn, String ontology, 
			String accession, String name) {
		this();
		this.type = type;
		this.ontology = ontology;
		this.accession = accession;
		this.name = name;
	}
	
	
	/**
	 * many-to-many: another Cv can have common children/members with this
	 */
	@ManyToMany(cascade = {CascadeType.ALL}, targetEntity = Cv.class)
	@JoinTable(name = "cv_member")
	public Set<Cv> getMembers() {
		return members;
	}

	public void setMembers(Set<Cv> members) {
		this.members = members;
	}

	public void addMember(Cv member) {
		this.members.add(member);
	}
	
	public void removeMember(Cv member) {
		this.members.remove(member);
	}

	@Column(length = 50, nullable = false)
	public String getOntology() {
		return ontology;
	}

	public void setOntology(String ontology) {
		this.ontology = ontology;
	}

	@Column(length = 50, nullable = false)
	public String getAccession() {
		return accession;
	}

	public void setAccession(String accession) {
		this.accession = accession;
	}

	@Column(length = 50)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	
	@CollectionOfElements
	@Column(name = "synonyms_x", columnDefinition = "text")
	@FieldBridge(impl = StringSetBridge.class)
	@Field(name = "synonyms", index = Index.TOKENIZED)
	public Set<String> getSynonyms() {
		return synonyms;
	}

	public void setSynonyms(Set<String> synonyms) {
		this.synonyms = synonyms;
	}

	
	@Column(length = 500, nullable=false, unique=true)
	public String getUrn() {
		return urn;
	}

	public void setUrn(String urn) {
		this.urn = urn;
	}

	@Column(length = 50, nullable=false)
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Cv && getUrn().equals(((Cv)obj).getUrn());
	}
	
	@Override
	public int hashCode() {
		return urn.hashCode();
	}
}
