package cpath.service.jpa;


import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import cpath.service.*;
import org.apache.commons.lang3.StringUtils;
import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Provenance;
import org.biopax.paxtools.model.level3.Score;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;


/**
 * Data Provider Metadata.
 */
@Entity
@DynamicUpdate
@DynamicInsert
@Table(name = "metadata")
public final class Metadata {

  // for metadata reading from a plain config. file
  public static final int METADATA_IDENTIFIER_INDEX = 0;
  public static final int METADATA_NAME_INDEX = 1;
  public static final int METADATA_DESCRIPTION_INDEX = 2;
  public static final int METADATA_DATA_URL_INDEX = 3;
  public static final int METADATA_HOMEPAGE_URL_INDEX = 4;
  public static final int METADATA_ICON_URL_INDEX = 5;
  public static final int METADATA_TYPE_INDEX = 6;
  public static final int METADATA_CLEANER_CLASS_NAME_INDEX = 7;
  public static final int METADATA_CONVERTER_CLASS_NAME_INDEX = 8;
  public static final int METADATA_PUBMEDID_INDEX = 9;
  public static final int METADATA_AVAILABILITY_INDEX = 10;
  public static final int NUMBER_METADATA_ITEMS = 11;

  private static final Pattern BAD_ID_PATTERN = Pattern.compile("\\s|-");

  // METADATA_TYPE Enum
  public enum METADATA_TYPE {
    // data types
    PSI_MI(true), // interactions to be converted to BioPAX L3 format
    PSI_MITAB(true), // interactions to be converted to PSI-MI then to BioPAX L3 format
    BIOPAX(true), // pathways and interactions in BioPAX L2 or L3 format
    SBML(true), // SBML (requires a data source specific Converter to BioPAX)
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

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  @Column(length = 40, unique = true, nullable = false)
  public String identifier;

  @NotEmpty
  @ElementCollection(fetch = FetchType.EAGER)
  @JoinTable(name = "metadata_name")
  @OrderColumn
  private List<String> name;

  @Column(nullable = false)
  private String description;

  private String urlToData;

  @Column(nullable = false)
  private String urlToHomepage;

  @Column(nullable = false)
  private String iconUrl;

  @NotNull
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private METADATA_TYPE type;

  private String cleanerClassname;
  private String converterClassname;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
  @JoinColumn(name = "metadata_id")
  private Set<Content> content;

  private String pubmedId;
  private String availability;
  private Integer numPathways;
  private Integer numInteractions;
  private Integer numPhysicalEntities;

  /**
   * Default Constructor.
   */
  Metadata() {
    content = new HashSet<>();
  }

  /**
   * Create a Metadata obj with the specified properties;
   *
   * @param identifier         unique short string, will be used in URIs
   * @param name               the not empty list of names: display name (must present), standard name, other names.
   * @param description        description of the data source (details, release date, version, etc.)
   * @param urlToData          URL - where the data can be download (can be part of larger data archive)
   * @param urlToHomepage      provider's home page URL
   * @param urlToLogo          provider's logo image URL
   * @param metadata_type      what kind of data (warehouse, biopax, psi-mi, id-mapping)
   * @param cleanerClassname   canonical name of a java class that implements {@link Cleaner}
   * @param converterClassname canonical name of a java class that implements {@link cpath.service.Converter}
   * @param pubmedId           recommended by the data provider reference publication PMID
   * @param availability       data availability: free, academic, not-free
   */
  public Metadata(final String identifier, final List<String> name, final String description,
                  final String urlToData, String urlToHomepage, final String urlToLogo,
                  final METADATA_TYPE metadata_type, final String cleanerClassname,
                  final String converterClassname,
                  final String pubmedId, final String availability) {
    this();
    setIdentifier(identifier);
    if (name == null || name.isEmpty())
      throw new IllegalAccessError("no names provided");
    setName(name);
    setDescription(description);
    setUrlToData(urlToData);
    setUrlToHomepage(urlToHomepage);
    setIconUrl(urlToLogo);
    setType(metadata_type);
    setCleanerClassname(cleanerClassname);
    setConverterClassname(converterClassname);
    setPubmedId(pubmedId);
    setAvailability(availability);
  }

  public Metadata(final String identifier, final String name, final String description,
                  final String urlToData, String urlToHomepage, final String urlToLogo,
                  final METADATA_TYPE metadata_type, final String cleanerClassname,
                  final String converterClassname,
                  final String pubmedId, final String availability) {
    this(identifier, Arrays.asList(name.split("\\s*;\\s*")), description, urlToData,
        urlToHomepage, urlToLogo, metadata_type, cleanerClassname, converterClassname,
        pubmedId, availability);
  }

  //setter is for JPA and tests only:
  void setId(Long id) {
    this.id = id;
  }

  public Long getId() {
    return id;
  }


  public Set<Content> getContent() {
    return content;
  }

  /**
   * Sets the identifier.
   * No spaces, dashes, allowed.
   *
   * @param identifier metadata identifier
   * @throws IllegalArgumentException if it's null, empty string, or contains spaces or dashes
   */
  void setIdentifier(String identifier) {
    // validate the parameter
    if (identifier == null
        || identifier.length() == 0
        || BAD_ID_PATTERN.matcher(identifier).find())
      throw new IllegalAccessError("Bad metadata identifier: " + identifier);

    // copy value
    this.identifier = identifier;
  }

  /**
   * Data source metadata identifier.
   * <p>
   * It can be also used as filter ('datasource')
   * value in cpath2 full-text search queries
   * (for pathway datasource types only)
   *
   * @return
   */
  public String getIdentifier() {
    return identifier;
  }

  /**
   * Sets data provider/source name.
   * <p>
   * Please use a standard name for pathway/interaction data types,
   * if possible (for warehouse data it's not so important),
   * as this will be recommended to use as filter ('datasource')
   * value in cpath2 full-text search queries
   *
   * @param name
   * @throws IllegalArgumentException
   */
  public void setName(List<String> name) {
    if (name == null) {
      throw new IllegalArgumentException("name must not be null");
    }
    this.name = name;
  }

  /**
   * Gets the data provider/source name.
   *
   * @return
   */
  public List<String> getName() {
    return name;
  }


  public void setDescription(String releaseDate) {
    if (releaseDate == null) {
      throw new IllegalArgumentException("release data must not be null");
    }
    this.description = releaseDate;
  }

  public String getDescription() {
    return description;
  }

  public void setUrlToData(String urlToData) {
    this.urlToData = urlToData;
  }

  public String getUrlToData() {
    return urlToData;
  }

  public void setUrlToHomepage(String urlToHomepage) {
    this.urlToHomepage = urlToHomepage;
  }

  public String getUrlToHomepage() {
    return urlToHomepage;
  }


  public void setType(METADATA_TYPE metadata_type) {
    if (metadata_type == null) {
      throw new IllegalArgumentException("type must not be null");
    }
    this.type = metadata_type;
  }

  public METADATA_TYPE getType() {
    return type;
  }

  public void setCleanerClassname(String cleanerClassname) {
    if (cleanerClassname == null || cleanerClassname.trim().length() == 0)
      this.cleanerClassname = null;
    else
      this.cleanerClassname = cleanerClassname.trim();
  }

  public String getCleanerClassname() {
    return (cleanerClassname == null || cleanerClassname.length() == 0)
        ? null : cleanerClassname;
  }

  public void setConverterClassname(String converterClassname) {
    if (converterClassname == null || converterClassname.trim().length() == 0)
      this.converterClassname = null;
    else
      this.converterClassname = converterClassname.trim();
  }

  public String getConverterClassname() {
    return (converterClassname == null || converterClassname.length() == 0)
        ? null : converterClassname;
  }


  @Override
  public String toString() {
    return identifier;
  }


  /**
   * Creates a new Provenance from this Metadata and sets
   * if to all Entity class objects in the model.
   * <p>
   * Removes all other Provenance instances and
   * corresponding dataSource property values
   * from the model.
   *
   * @param model BioPAX model to update
   */
  public void setProvenanceFor(Model model) {
    Provenance pro = null;

    // we create URI from the Metadata identifier and version.
    final String uri = model.getXmlBase() + identifier;
    pro = (model.containsID(uri))
        ? (Provenance) model.getByID(uri)
        : model.addNew(Provenance.class, uri);

    // parse/set names
    String displayName = getName().iterator().next();
    pro.setDisplayName(displayName);
    pro.setStandardName(standardName());

    if (getName().size() > 2)
      for (int i = 2; i < getName().size(); i++)
        pro.addName(getName().get(i));

    // add additional info about the current version, source, identifier, etc...
    final String loc = getUrlToData();
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
   * @return
   */
  public String standardName() {
    //also capitalize (can be extremely useful...)
    if (name.size() > 1)
      return StringUtils.capitalize(name.get(1));
    else
      return StringUtils.capitalize(name.get(0));
  }

  public Integer getNumPathways() {
    return numPathways;
  }

  public void setNumPathways(Integer numPathways) {
    this.numPathways = numPathways;
  }

  public Integer getNumInteractions() {
    return numInteractions;
  }

  public void setNumInteractions(Integer numInteractions) {
    this.numInteractions = numInteractions;
  }

  public Integer getNumPhysicalEntities() {
    return numPhysicalEntities;
  }

  public void setNumPhysicalEntities(Integer numPhysicalEntities) {
    this.numPhysicalEntities = numPhysicalEntities;
  }

  public String getPubmedId() {
    return pubmedId;
  }

  public void setPubmedId(String pubmedId) {
    this.pubmedId = pubmedId;
  }

  public String getAvailability() {
    return availability;
  }

  public void setAvailability(String availability) {
    this.availability = availability;
  }

  public String getIconUrl() {
    return iconUrl;
  }

  public void setIconUrl(String iconUrl) {
    this.iconUrl = iconUrl;
  }

  @Transient
  public boolean isNotPathwayData() {
    return type.isNotPathwayData();
  }

  public void setNotPathwayData(boolean foo) {
    //a fake bean property (for javascript, JSON)
  }

  @Override
  public boolean equals(Object o) {
    return (o instanceof Metadata) && identifier.equals(((Metadata) o).getIdentifier());
  }

  @Override
  public int hashCode() {
    return identifier.hashCode();
  }
}
