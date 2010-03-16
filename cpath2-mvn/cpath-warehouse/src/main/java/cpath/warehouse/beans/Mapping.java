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

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

import cpath.warehouse.CPathWarehouse;


/**
 * This is to make a URN-to-primaryURN 
 * id-map (for CV's and ER's identifiers)
 * 
 * @author rodch
 *
 */
@Entity
@Table(name = "mapping")
@Indexed(index=CPathWarehouse.SEARCH_INDEX_NAME)
@NamedQueries({
  @NamedQuery(
    name="cpath.warehouse.beans.searchLeft",
    query="from cpath.warehouse.beans.Mapping as mapp where upper(mapp.leftUrn) = upper(:urn)"
  ),
  @NamedQuery(
    name="cpath.warehouse.beans.searchRight",
    query="from cpath.warehouse.beans.Mapping as mapp where upper(mapp.rightUrn) = upper(:urn)"
  ),
  @NamedQuery(
    name="cpath.warehouse.beans.searchByMappingType",
    query="from cpath.warehouse.beans.Mapping as mapp where upper(mapp.mappingType) = upper(:maptype)"
  )
})
public class Mapping implements Serializable {
	private static final long serialVersionUID = 1L;

	public static final String SEARCH_FIELD_THIS = "leftUrn";
	public static final String SEARCH_FIELD_THAT = "rightUrn";
	public static final String SEARCH_FIELD_TYPE = "maptype";
	
	/**
	 * Mapping Type
	 * 
	 * @author rodche
	 *
	 */
	public enum MappingType {
		IDENTITY, RELATIONSHIP;
	}
	
	
	@Id
    @GeneratedValue
    @Column(name="urn_id")
	private Integer id = 0;
	
	@Column(nullable=false)
	@Field(name=SEARCH_FIELD_THIS)
	private String leftUrn;
	
	@Column(nullable=false)
	@Field(name=SEARCH_FIELD_THAT)
	private String rightUrn;
	
	@Column(nullable=false)
	@Field(name=SEARCH_FIELD_TYPE)
	@Enumerated(EnumType.STRING)
	private MappingType mappingType;
	

	public Integer getId() {
		return id;
	}

	public void setId(Integer value) {
		id = value;
	}
	
	
	public Mapping() {}
	
	
	public Mapping(String left, String right, MappingType mappingType) {
		this.leftUrn = left;
		this.rightUrn = right;
		this.mappingType = mappingType;
	}

	public String getLeftUrn() {
		return leftUrn;
	}

	public void setLeftUrn(String left) {
		this.leftUrn = left;
	}

	public String getRightUrn() {
		return rightUrn;
	}

	public void setRightUrn(String right) {
		this.rightUrn = right;
	}

	public MappingType getMappingType() {
		return mappingType;
	}

	public void setMappingType(MappingType mappingType) {
		this.mappingType = mappingType;
	}
	
}
