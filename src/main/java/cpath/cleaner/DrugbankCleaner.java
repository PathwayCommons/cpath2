package cpath.cleaner;

import cpath.service.api.Cleaner;

import java.io.InputStream;
import java.io.OutputStream;

public class DrugbankCleaner implements Cleaner {

  /*
  * drugbank biopax data uses the following values in xref.db properties,
  * which biopax validator reports as "Unknown";
  * so we need to replace with the corresponding standard prefix/name from bioregistry.io (or identifiers.org):
  GenBank Gene Database -> "genbank"
  GenBank Protein Database -> "genbank" (numeric IDs, not like it's in ncbiprotein)
  Therapeutic Targets Database ->
  Guide to Pharmacology ->
  HUGO Gene Nomenclature Committee (HGNC) -> "hgnc.symbol" (or "HGNC Symbol")
  IUPHAR ->
  Drugs Product Database (DPD) ->
   */

  @Override
  public void clean(InputStream data, OutputStream cleanedData) {
    //TODO: implement
  }
}
