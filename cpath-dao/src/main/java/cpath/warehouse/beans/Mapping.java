package cpath.warehouse.beans;


import java.util.Map;

import javax.persistence.*;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

/**
 * A simple id-mapping db table to
 * unambiguously associate a chemical or bio 
 * identifier with the corresponding primary 
 * ChEBI ID or UniProt AC, respectively.
 * 
 * @author rodche
 */
@Entity
@DynamicUpdate
@DynamicInsert
@Table(name="mapping")
@NamedQueries({
	@NamedQuery(name = "cpath.warehouse.beans.mappingsByType", 
		query = "from Mapping where type = :type order by id")
})
public final class Mapping {

	public static enum Type {
		UNIPROT,
		CHEBI,
	}
		
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Integer id;
	
	@Enumerated
    private Type type;
	
	@Column
	String description;
	
	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name="mapping_map")
	@OrderColumn
	//make map columns case-sensitive in order to properly handle bio/chem. identifiers!
	@MapKeyColumn(name="identifier", columnDefinition="varchar(30) character set utf8 collate utf8_bin NOT NULL")
	@Column(name="accession", columnDefinition="varchar(30) character set utf8 collate utf8_bin NOT NULL")
    private Map<String,String> map;

	/**
	 * Default Constructor.
	 */
	public Mapping() {}

 
	/**
	 * Constructor.
	 * 
	 * @param type
	 * @param map
	 * 
	 * @throws IllegalArgumentException when type or map is null
	 */
    public Mapping(final Mapping.Type type, final String description, final Map<String,String> map) {
    	if(type == null)
    		throw new IllegalArgumentException("Type is null");
    	if(map == null)
    		throw new IllegalArgumentException("Map is null");
    	
    	this.type = type;
    	this.map = map;
    	this.description = description;
    }
    
    
    public Integer getId() {
		return id;
	}
    //used only by the framework or tests
	void setId(Integer id) {
		this.id = id;
	}
	

	public Mapping.Type getType() {
		return type;
	}
	void setType(Mapping.Type type) {
		this.type = type;
	}


	public Map<String, String> getMap() {
		return map;
	}
	void setMap(Map<String, String> map) {
		this.map = map;
	}

	
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}


	@Override
    public String toString() {
    	return type + "" + id + "; " + description;
    }
    
}
