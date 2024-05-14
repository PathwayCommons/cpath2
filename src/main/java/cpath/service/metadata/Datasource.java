package cpath.service.metadata;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Provenance;
import org.biopax.paxtools.model.level3.Score;

/**
 * Data provider/source metadata.
 *
 * Node: some public getters and setters below, despite java warnings, are in fact called from the
 * web view layer (e.g., JSP) or when a web controller returns JSON/XML object.
 */
@Data //Lombok auto-generates getter/setters, toString, equals, hashCode unless already defined
@NoArgsConstructor
@AllArgsConstructor
public final class Datasource {

  private static final Pattern BAD_ID_PATTERN = Pattern.compile("\\s|-");

  // METADATA_TYPE Enum
  public enum METADATA_TYPE {
    // data types
    PSI_MI(true), // interactions to be converted to BioPAX L3 format
    PSI_MITAB(true), // interactions to be converted to PSI-MI then to BioPAX L3 format
    BIOPAX(true), // pathways and interactions in BioPAX L2 or L3 format
    WAREHOUSE(false), // warehouse data to be converted to BioPAX and used during the merge stage
    MAPPING(false); //extra gene/protein id-mapping data (two column, TSV format: "some id or name" \t "primary uniprot/chebi AC")

    private final boolean pathwayData;

    METADATA_TYPE(boolean isPathwayData) {
      this.pathwayData = isPathwayData;
    }

    public boolean isNotPathwayData() {
      return !pathwayData;
    }
  }

  //the order of fields here matters as the all-args Constructor will be auto-generated!
  private String identifier;
  private List<String> name; //data provider standard names
  private String description;
  private String dataUrl;
  private String homepageUrl;
  private String iconUrl;
  private METADATA_TYPE type;
  private String cleanerClass;
  private String converterClass;
  @JsonIgnore private Set<String> files;
  private String pubmedId;
  private String availability;
  private int numPathways;
  private int numInteractions;
  private int numPhysicalEntities;


  public Set<String> getFiles() {
    if(files == null) {
      files = new HashSet<>();
    }
    return files;
  }

  /**
   * Sets the identifier.
   * No spaces, dashes, allowed.
   *
   * @param identifier metadata identifier
   * @throws IllegalArgumentException if it's null, empty string, or contains spaces or dashes
   */
  public void setIdentifier(@NonNull String identifier) {
    if (StringUtils.isBlank(identifier) || BAD_ID_PATTERN.matcher(identifier).find()) {
      throw new IllegalArgumentException("Bad metadata identifier: " + identifier);
    }
    this.identifier = identifier;
  }

  @Override
  public String toString() {
    return identifier;
  }

  /**
   * Creates a new Provenance from this Datasource and sets
   * if to all Entity class objects in the model.
   * <p>
   * Removes all other Provenance instances and
   * corresponding dataSource property values
   * from the model.
   *
   * @param model BioPAX model to update
   * @param xmlBase xml:base to use for the Provenance
   */
  public void setProvenanceFor(Model model, String xmlBase) {
    Provenance pro;

    // we create URI from the Datasource identifier and version.
    final String uri = xmlBase + identifier;
    pro = (model.containsID(uri))
        ? (Provenance) model.getByID(uri)
        : model.addNew(Provenance.class, uri);

    // parse/set names
    String displayName = getName().iterator().next();
    pro.setDisplayName(displayName);
    pro.setStandardName(standardName());
    pro.addName(identifier);

    if (getName().size() > 2)
      for (int i = 2; i < getName().size(); i++)
        pro.addName(getName().get(i));

    // add additional info about the current version, source, identifier, etc...
    final String loc = getDataUrl();
    pro.addComment("Source " +
        //skip for a local or empty (default) location
        ((loc.startsWith("http:") || loc.startsWith("ftp:")) ? loc : "")
        + " type: " + getType() + ", " + getDescription());

    // replace for all entities
    for (org.biopax.paxtools.model.level3.Entity ent : model.getObjects(org.biopax.paxtools.model.level3.Entity.class)) {
      for (Provenance ds : new HashSet<>(ent.getDataSource()))
        ent.removeDataSource(ds);
      ent.addDataSource(pro);
    }

    for (Score score : model.getObjects(Score.class))
      if (score.getScoreSource() == null)
        score.setScoreSource(pro);

    // remove dangling Provenance from the model
    ModelUtils.removeObjectsIfDangling(model, Provenance.class);
  }

  /**
   * Returns the standard name (the second one in the name list),
   * if present, otherwise - returns the first name (display name)
   *
   * @return name
   */
  public String standardName() {
    //also capitalize (can be extremely useful...)
    if (name.size() > 1)
      return StringUtils.capitalize(name.get(1));
    else
      return StringUtils.capitalize(name.get(0));
  }

  @Override
  public boolean equals(Object o) {
    return (o instanceof Datasource) && identifier.equals(((Datasource) o).getIdentifier());
  }

  @Override
  public int hashCode() {
    return identifier.hashCode();
  }
}
