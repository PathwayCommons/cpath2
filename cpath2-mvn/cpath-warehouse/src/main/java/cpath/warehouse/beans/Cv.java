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
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
@Entity
@Table(name = "cv")
@Indexed(index=CPathWarehouse.SEARCH_INDEX_NAME)
@NamedQueries({
  @NamedQuery(
    name="cpath.warehouse.beans.cvByUrn",
    query="from cpath.warehouse.beans.Cv as vocab where upper(vocab.urn) = upper(:urn)"
  )
})
public class Cv implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String URN_OBO_PREFIX = "urn:miriam:obo.";
	
	public static final String SEARCH_FIELD_PARENTS="parents";
	public static final String SEARCH_FIELD_CHILDREN="children";
	public static final String SEARCH_FIELD_URN="urn";
	public static final String SEARCH_FIELD_NAMES="name";
	
	@Id
    @GeneratedValue
    @Column(name="cv_id")
	private Integer id = 0;
	
	@Column(name="urn", length = 500, nullable=false, unique=true)
	@Field(name = SEARCH_FIELD_URN)
	private String urn;
	
	@CollectionOfElements
	@Column(name = "children_x", columnDefinition = "text")
	@FieldBridge(impl = StringSetBridge.class)
	@Field(name = SEARCH_FIELD_CHILDREN, index = Index.TOKENIZED)
	private Set<String> children;
	
	@CollectionOfElements
	@Column(name = "parents_x", columnDefinition = "text")
	@FieldBridge(impl = StringSetBridge.class)
	@Field(name = SEARCH_FIELD_PARENTS, index = Index.TOKENIZED)
	private Set<String> parents;
	
	@Transient
	private String accession;
	@Transient
	private String ontologyId;
	
	@CollectionOfElements
	@Column(name = "names_x", columnDefinition = "text")
	@FieldBridge(impl = StringSetBridge.class)
	@Field(name = SEARCH_FIELD_NAMES, index = Index.TOKENIZED)
	private List<String> names;
	

	public Cv() {
		children = new HashSet<String>();
		parents = new HashSet<String>();
		names = new ArrayList<String>();
	}
	
	public Cv(String urn) {
		this();
		this.urn = urn;
		
		if(urn.startsWith(URN_OBO_PREFIX)) {
			int l = URN_OBO_PREFIX.length();
			int indexOfTheColon = urn.indexOf(':', l);
			String ont = urn.substring(l, indexOfTheColon);
			String acc = urn.substring(indexOfTheColon+1);
			acc=URLDecoder.decode(acc);
			this.accession = acc;
			this.ontologyId = ont;
		} else {
			throw new IllegalArgumentException(
					"Not a CV URN : " + urn);
		}
		
	}
	
	public Integer getId() {
		return id;
	}

	public void setId(Integer value) {
		id = value;
	}
	
	
	public Set<String> getParents() {
		return parents;
	}
	
	public void setParents(Set<String> parents) {
		this.parents = parents;
	}
	
	
	public Set<String> getChildren() {
		return children;
	}

	public void setChildren(Set<String> children) {
		this.children = children;
	}

	public String getUrn() {
		return urn;
	}

	public void setUrn(String urn) {
		this.urn = urn;
	}


	@Override
	public boolean equals(Object obj) {
		return obj instanceof Cv && getUrn().equals(((Cv)obj).getUrn());
	}
	
	@Override
	public int hashCode() {
		return urn.hashCode();
	}

	@Transient
	public String getAccession() {
		return accession;
	}

	@Transient
	public String getOntologyId() {
		return ontologyId;
	}

	public List<String> getNames() {
		return names;
	}

	public void setNames(List<String> names) {
		this.names = names;
	}

	public void addName(String name) {
		this.names.add(name);
	}
}
