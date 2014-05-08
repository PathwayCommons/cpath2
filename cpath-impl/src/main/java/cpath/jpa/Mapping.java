package cpath.jpa;

import javax.persistence.*;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.util.Assert;

/**
 * A simple id-mapping db table.
 * 
 * @author rodche
 */
@Entity
@DynamicUpdate
@DynamicInsert
@Table(name="mapping", uniqueConstraints = @UniqueConstraint(columnNames = {"src", "srcId", "dest", "destId"}))
public final class Mapping {
		
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Integer id;
	
	@Column(nullable=false)
	private String src; 
	
	@Column(nullable=false)
	private String dest;
		
	@Column(nullable=false)
	private String srcId; 
	
	@Column(nullable=false)
	private String destId;
	
	/**
	 * Default Constructor.
	 */
	public Mapping() {}

 
    public Mapping(String src, String srcId, String dest, String destId) 
    {
    	Assert.hasText(src);
    	Assert.hasText(srcId);
    	Assert.hasText(dest);
    	Assert.hasText(destId);
    	
    	this.src = src.toUpperCase();
    	this.dest = dest.toUpperCase();
    	this.srcId = srcId;
    	this.destId = destId;
    }
    
    

	public String getSrc() {
		return src;
	}
	void setSrc(String src) {
		this.src = src.toUpperCase();
	}


	public String getDest() {
		return dest;
	}
	void setDest(String dest) {
		this.dest = dest.toUpperCase();
	}


	public String getSrcId() {
		return srcId;
	}
	void setSrcId(String srcId) {
		this.srcId = srcId;
	}


	public String getDestId() {
		return destId;
	}
	void setDestId(String destId) {
		this.destId = destId;
	}


	@Override
    public String toString() {
    	return "Mapping from " + src + ", " + srcId + " to " + dest + ", " + destId;
    } 
}
