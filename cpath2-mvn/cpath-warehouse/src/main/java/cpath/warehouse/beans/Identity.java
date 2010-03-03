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

import javax.persistence.*;

import org.hibernate.search.annotations.Indexed;

import cpath.warehouse.CPathWarehouse;


/**
 * This is to make a URN-to-primaryURN 
 * id-map (for CV's and ER's identifiers)
 * 
 * @author rodch
 *
 */
@Entity(name = "identity")
@Indexed(index=CPathWarehouse.SEARCH_INDEX_NAME)
public class Identity implements Serializable {

	private static final long serialVersionUID = 1L;

	private Integer id = 0;
	
	@Id
    @GeneratedValue
    @Column(name="urn_id")
	public Integer getId() {
		return id;
	}

	public void setId(Integer value) {
		id = value;
	}
	
	private String urn;
	private Identity primaryUrn;
	
	public Identity() {}

	@Column(nullable=false, unique=true)
	public String getUrn() {
		return urn;
	}

	public void setUrn(String urn) {
		this.urn = urn;
	}

	@ManyToOne(cascade = {CascadeType.ALL})
	@JoinColumn(name="primary_urn")
	public Identity getPrimaryUrn() {
		return primaryUrn;
	}

	public void setPrimaryUrn(Identity primaryUrn) {
		this.primaryUrn = primaryUrn;
	}

	
}
