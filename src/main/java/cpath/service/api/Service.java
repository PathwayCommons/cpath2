package cpath.service.api;


import java.util.Collection;
import java.util.Map;
import java.util.Set;

import cpath.service.Settings;
import cpath.service.metadata.Datasource;
import cpath.service.metadata.Index;
import org.biopax.paxtools.controller.PathAccessor;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.query.algorithm.Direction;

import cpath.service.metadata.Mappings;
import cpath.service.metadata.Metadata;
import cpath.service.jaxb.ServiceResponse;
import org.biopax.validator.api.beans.Validation;


/**
 * A middle-tier interface that defines data and metadata access and analysis methods.
 *
 * @author rodche
 */
public interface Service {

  Model getModel();

  void setModel(Model paxtoolsModel);

  /**
   * Retrieves the BioPAX element(s) by URI or identifier (e.g., gene symbol)
   * - a complete BioPAX sub-model with all available child elements and
   * properties - and then converts it to the specified output format
   * (if applicable), such as BioPAX (RDF/XML), SIF, GSEA (.gmt).
   * @param format output format type
   * @param formatOptions format parameters
   * @param subPathways optional, include/skip (default) sub-pathways
   * @param uris the list of URIs to fetch  @return
   */
  ServiceResponse fetch(OutputFormat format, Map<String, String> formatOptions,
                        boolean subPathways, String... uris);

  /**
   * Full-text search for the BioPAX elements.
   *
   * @param queryStr search expression (a keyword or Lucene query string)
   * @param page search results page no.
   * @param biopaxClass biopax type (interface, such as Pathway, Complex)
   * @param dsources URIs of data sources
   * @param organisms URIs of organisms
   * @return search/error response
   */
  ServiceResponse search(String queryStr,
                         int page, Class<? extends BioPAXElement> biopaxClass, String[] dsources, String[] organisms);

  /**
   * Runs a neighborhood query using the given parameters
   * (returns a sub-model in the specified format,
   * wrapped as service object).
   *  @param format output format type
   * @param formatOptions format options
   * @param sources IDs of seed of neighborhood
   * @param limit search limit (integer value)
   * @param direction flag
   * @param organisms optional filter
   * @param datasources optional filter
   * @param subPathways  optional, include/skip sub-pathways; it does not affect the graph search algorithm,
   */
  ServiceResponse getNeighborhood(OutputFormat format,
                                  Map<String, String> formatOptions,
                                  String[] sources, Integer limit, Direction direction,
                                  String[] organisms, String[] datasources, boolean subPathways);

  /**
   * Runs a paths-between query for the given sources
   * (returns a sub-model in the specified format,
   * wrapped as service object).
   *  @param format output format type
   * @param formatOptions format options
   * @param sources IDs of source molecules
   * @param limit search limit (integer value)
   * @param organisms optional filter
   * @param datasources optional filter
   * @param subPathways optional, include/skip sub-pathways; it does not affect the graph search algorithm,
   */
  ServiceResponse getPathsBetween(OutputFormat format, Map<String, String> formatOptions,
                                  String[] sources, Integer limit, String[] organisms,
                                  String[] datasources, boolean subPathways);

  /**
   * Runs a POI query from the given sources to the given targets
   * (returns a sub-model in the specified format, wrapped as service object).
   *  @param format output format
   * @param formatOptions format options
   * @param sources IDs of source molecules
   * @param targets IDs of target molecules
   * @param limit search limit (integer value)
   * @param organisms optional filter
   * @param datasources optional filter
   * @param subPathways optional, include/skip sub-pathways; it does not affect the graph search algorithm,
   */
  ServiceResponse getPathsFromTo(OutputFormat format, Map<String, String> formatOptions,
                                 String[] sources, String[] targets, Integer limit, String[] organisms,
                                 String[] datasources, boolean subPathways);

  /**
   * Runs a common upstream or downstream query
   * (returns a sub-model in the specified format,
   * wrapped as service object).
   *  @param format output format
   * @param formatOptions format options
   * @param sources IDs of query seed
   * @param limit search limit
   * @param direction - can be {@link Direction#DOWNSTREAM} or {@link Direction#UPSTREAM}
   * @param organisms optional filter
   * @param datasources optional filter
   * @param subPathways optional, include/skip sub-pathways; it does not affect the graph search algorithm,
   */
  ServiceResponse getCommonStream(OutputFormat format, Map<String, String> formatOptions,
                                  String[] sources, Integer limit, Direction direction,
                                  String[] organisms, String[] datasources, boolean subPathways);

  //---------------------------------------------------------------------------------------------|

  /**
   * Collects BioPAX property values at the end of the property path
   * applied to each BioPAX object in the list (defined by URIs),
   * where applicable.
   *
   * @see PathAccessor
   *
   * @param propertyPath Paxtools' BioPAX object property path (path accessor) expression
   * @param sourceUris URIs of model elements to start traversing from
   * @return traverse or error response bean
   */
  ServiceResponse traverse(String propertyPath, String... sourceUris);

  /**
   * Lists (some non-trivial) parent pathways in the current BioPAX model.
   *
   * @param q query string (keywords or Lucene syntax query string)
   * @param organisms filter values (URIs, names, or taxonomy IDs)
   * @param datasources filter values (URIs, names)
   * @return top pathways or error response object
   */
  ServiceResponse topPathways(String q, String[] organisms, String[] datasources);

  /**
   * Maps multiple identifiers to primary IDs of given type.
   * Auto-detects the source ID type or tries all types.
   * The result set may contain more than one primary ID.
   *
   * @param fromIds the source IDs
   * @param toDb standard preferred name of the target ID type/collection name/prefix (e.g., 'UNIPROT','CHEBI')
   * @return a set of primary IDs of the type; normally one or none elements
   */
  Set<String> map(Collection<String> fromIds, String toDb);

  /**
   * Record web service and data access events.
   * @param ip IP address
   * @param category log event category
   * @param name event name
   */
  void track(String ip, String category, String name);

  Mappings mapping();

  Metadata metadata();

  Index index();

  /**
   * in production, loads the pre-built biopax model and the corresponding full-text index
   * (location depends on application.properties) and the blacklist file.
   */
  void init();

  /**
   * Loads a biopax model, metadata and the full-text index (existing or to generate)
   *
   * @param model
   * @param indexLocation
   * @param readOnly
   */
  void initIndex(Model model, String indexLocation, boolean readOnly);

  // Datasource and data processing methods

  /**
   * Clears the datasource object, drops/creates the data directory.
   *
   * @param datasource data source datasource
   */
  void clear(Datasource datasource);

  Model loadMainModel();

  Model loadWarehouseModel();

  Model loadBiopaxModelByDatasource(Datasource datasource);

  String getDataArchiveName(Datasource datasource);

  String intermediateDataDir(Datasource datasource);

  /**
   * Given data source, this procedure expands the corresponding
   * original data archive (zip), collecting the data file names.
   * @param datasource Datasource
   */
  void unzipData(Datasource datasource);

  void saveValidationReport(Validation v, String reportFile);

  Settings settings();

  void setSettings(Settings settings); //mainly for tests
}
