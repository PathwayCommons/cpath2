package cpath.cleaner;

import cpath.service.api.Cleaner;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Xref;

import java.io.InputStream;
import java.io.OutputStream;

public class DrugbankCleaner implements Cleaner {
  /*
  * drugbank biopax data uses the following weird values in xref.db properties
  * (which biopax validator reports as "Unknown"; so we need to map to standard names from bioregistry.io)
  * - GenBank Gene Database -> "genbank"
  * - GenBank Protein Database -> "genbank" (numeric IDs, not like it's in ncbiprotein)
  * - Therapeutic Targets Database -> ttd.drug
  * - HUGO Gene Nomenclature Committee (HGNC) -> "hgnc" (e.g. HGNC:1111)
  * - Drugs Product Database (DPD) -> cdpd
  * - IUPHAR -> iuphar.family or iuphar.ligand or iuphar.receptor?
  * - Guide to Pharmacology -> likely same as IUPHAR (but which of the three collections?)
  */
  @Override
  public void clean(InputStream data, OutputStream cleanedData) {
    //TODO: implement
    try
    {
      SimpleIOHandler simpleReader = new SimpleIOHandler(BioPAXLevel.L3);
      Model model = simpleReader.convertFromOWL(data);
      cleanXrefDBName(model);
      simpleReader.convertToOWL(model, cleanedData);
    } catch (Exception e) {
      throw new RuntimeException("HumanCycCleaner failed", e);
    }
  }

  protected void cleanXrefDBName(Model model)
  {
    for (Xref xr : model.getObjects(Xref.class))
    {
      if(xr.getDb() == null) {
        //skip
      }
      else if(xr.getDb().equalsIgnoreCase("GenBank Gene Database")) {
        xr.setDb("genbank");
      }
      else if(xr.getDb().equalsIgnoreCase("GenBank Protein Database")) {
        xr.setDb("genbank");
      }
      else if(xr.getDb().equalsIgnoreCase("Therapeutic Targets Database")) {
        xr.setDb("ttd.drug");
        xr.addComment("Therapeutic Targets Database");
      }
      else if(xr.getDb().equalsIgnoreCase("HUGO Gene Nomenclature Committee (HGNC)")) { //HGNC:1234 ids
        xr.setDb("hgnc");
      }
      else if(xr.getDb().equalsIgnoreCase("Drugs Product Database (DPD)")) {
        xr.setDb("cdpd");
        xr.addComment("Drugs Product Database (DPD)");
      }
    }
  }

}
