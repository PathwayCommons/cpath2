package cpath.jpa;

import javax.persistence.*;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.util.Assert;

/**
 * Id-mapping Entity.
 * @author rodche
 */
@Entity
@DynamicUpdate
@DynamicInsert
@Table(
	name="mapping",
	uniqueConstraints = @UniqueConstraint(columnNames = {"src", "srcId", "dest", "destId"}),
	indexes = {
		@Index(name="src_index", columnList = "src"),
		@Index(name="srcId_index", columnList = "srcId"),
		@Index(name="dest_index", columnList = "dest"),
		@Index(name="destId_index", columnList = "destId"),
		@Index(name="dest_destId_index", columnList = "dest,destId"),
		@Index(name="srcId_dest_index", columnList = "srcId,dest"),
		@Index(name="src_srcId_dest_index", columnList = "src,srcId,dest"),
	}
)
public final class Mapping {
		
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	@Column(nullable=false, length = 35)
	private String src;
	
	@Column(nullable=false, length = 10) //now, it can be either 'CHEBI' or 'UNIPROT' only.
	private String dest;
		
	@Column(nullable=false, length = 50) //InChIKey ~27 sym (longest ID type); won't map names > 50 symbols
	private String srcId; 
	
	@Column(nullable=false, length = 15)
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
		//replace uniprot* db names with simply 'UNIPROT'
		if(this.src.startsWith("UNIPROT") || this.src.startsWith("SWISSPROT")) this.src = "UNIPROT";
		this.srcId = srcId;

		this.dest = dest.toUpperCase();
    	this.destId = destId;
    }
    
    
    Long getId() {
    	return id;
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
