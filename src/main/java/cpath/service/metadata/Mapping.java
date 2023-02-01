package cpath.service.metadata;

import cpath.service.CPathUtils;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.biopax.paxtools.controller.ModelUtils;


/**
 * Bio id-mapping entry.
 *
 * @author rodche
 */
@Data
@NoArgsConstructor
public final class Mapping {

  private String srcDb;
  private String dstDb;
  private String srcId;
  private String dstId;


  public Mapping(@NonNull String srcDb, @NonNull String srcId, @NonNull String dstDb, @NonNull String dstId) {
    srcDb = srcDb.toUpperCase();
    //replace an uniprot* db name with simply 'UNIPROT'
    if (srcDb.startsWith("UNIPROT") || srcDb.startsWith("SWISSPROT"))
      srcDb = "UNIPROT";
    else if (srcDb.startsWith("PUBCHEM") && (srcDb.contains("COMPOUND") || srcDb.contains("CID"))) {
      srcDb = "PUBCHEM-COMPOUND";
    } else if (srcDb.startsWith("PUBCHEM") && (srcDb.contains("SUBSTANCE") || srcDb.contains("SID"))) {
      srcDb = "PUBCHEM-SUBSTANCE";
    }
    this.srcDb = srcDb; //already upper case
    this.srcId = CPathUtils.fixSourceIdForMapping(srcDb, srcId);
    this.dstDb = dstDb.toUpperCase();
    this.dstId = dstId;
  }

  public String docId() {
    return ModelUtils.md5hex(toString());
  }

}
