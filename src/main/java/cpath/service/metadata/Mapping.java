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
    this.srcDb = srcDb.toUpperCase();
    this.srcId = CPathUtils.fixIdForMapping(srcDb, srcId);
    this.dstDb = dstDb.toUpperCase();
    this.dstId = CPathUtils.fixIdForMapping(dstDb, dstId);
  }

  public String docId() {
    return ModelUtils.md5hex(toString());
  }

}
