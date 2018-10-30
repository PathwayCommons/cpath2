package cpath.service.api;

import java.util.Set;

import org.biopax.paxtools.model.level3.ControlledVocabulary;

public interface CvRepository {

  /**
   * Gets a normalized BioPAX Controlled Vocabulary by standard (MIRIAM EBI) URI.
   *
   * @param <T>
   * @param uri     e.g., urn:miriam:obo.go:GO%3A0005654 or http://identifiers.org/obo.go/GO:0005654
   * @param cvClass
   * @return
   */
  <T extends ControlledVocabulary> T getControlledVocabulary(String uri, Class<T> cvClass);

  /**
   * Lookup for a CV of given class by ontology (name, synonym, or URI)
   * and accession (ID) or term name/synonym.
   *
   * @param <T>
   * @param db      OBO ontology name, synonym, or URI (e.g., "Gene Ontology", "go", "urn:miriam:obo.go", or "http://identifiers.org/obo.go/")
   * @param id      term's accession number (identifier) or name/synonym
   * @param cvClass
   * @return the controlled vocabulary or null (when no match found or ambiguous)
   */
  <T extends ControlledVocabulary> T getControlledVocabulary(String db, String id, Class<T> cvClass);


  /*
   * CVs Hierarchy Access
   */

  Set<String> getDirectChildren(String urn);

  Set<String> getDirectParents(String urn);

  Set<String> getAllChildren(String urn);

  Set<String> getAllParents(String urn);

  boolean isChild(String parentUrn, String urn);

}
