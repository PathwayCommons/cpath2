package cpath.service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

import cpath.service.api.OutputFormat;
import org.apache.commons.io.IOUtils;
import org.biopax.paxtools.io.gsea.GSEAConverter;
import org.biopax.paxtools.io.jsonld.JsonldBiopaxConverter;
import org.biopax.paxtools.io.jsonld.JsonldConverter;
import org.biopax.paxtools.io.sbgn.L3ToSBGNPDConverter;
import org.biopax.paxtools.io.sbgn.ListUbiqueDetector;
import org.biopax.paxtools.io.*;
import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.Provenance;
import org.biopax.paxtools.pattern.miner.*;
import org.biopax.paxtools.pattern.util.Blacklist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpath.service.jaxb.DataResponse;
import cpath.service.jaxb.ServiceResponse;
import org.springframework.util.Assert;

import static cpath.service.api.Status.*;

/**
 * A utility class to convert a BioPAX
 * L3 RDF/XML data stream or {@link Model}
 * to one of {@link OutputFormat}s
 * (including - to BioPAX L3 RDF/XML)
 *
 * @author rodche
 */
public class BiopaxConverter {
  private static final Logger log = LoggerFactory.getLogger(BiopaxConverter.class);

  private final Blacklist blacklist;

  /**
   * Constructor.
   *
   * @param blacklist of ubiquitous molecules to exclude (in some algorithms)
   */
  public BiopaxConverter(Blacklist blacklist) {
    //set an empty one if null
    this.blacklist = (blacklist != null) ? blacklist : new Blacklist();
  }


  /**
   * Converts the BioPAX data into the other format.
   *
   * @param m       paxtools model (not null)
   * @param format  output format
   * @param options format options
   * @param os      output stream
   */
  private void convert(Model m,
                       OutputFormat format,
                       Map<String, String> options,
                       OutputStream os) throws IOException {
    Assert.notNull(m, "Model is null");
    try {
      switch (format) {
        case BIOPAX: //to OWL (RDF/XML)
          (new SimpleIOHandler()).convertToOWL(m, os);
          break;
        case SIF:
          convertToSIF(m, os, false, options);
          break;
        case TXT:
          convertToSIF(m, os, true, options);
          break;
        case GSEA:
          convertToGSEA(m, os, options);
          break;
        case SBGN:
          //will do SBGN layout IIF value is "true" (case-insensitive)
          convertToSBGN(m, os, blacklist, Boolean.valueOf(options.get("layout")));
          break;
        case JSONLD:
          convertToJsonLd(m, os);
          break;
        default:
          throw new UnsupportedOperationException(
            "convert, yet unsupported format: " + format);
      }
    } finally {
      //makes sure OS is closed
      IOUtils.closeQuietly(os);
    }
  }

  private void convertToJsonLd(Model m, OutputStream os) throws IOException {
    DataResponse dr = (DataResponse) convert(m, OutputFormat.BIOPAX, null);
    JsonldConverter converter = new JsonldBiopaxConverter();
    Path data = (Path) dr.getData();
    InputStream is = Files.newInputStream(data, StandardOpenOption.DELETE_ON_CLOSE);
    converter.convertToJsonld(is, os);
    is.close();
  }

  /**
   * Converts not too large BioPAX model
   * (e.g., a graph query result) to another format.
   *
   * @param m       a sub-model (not too large), e.g., a get/graph query result
   * @param format  output format
   * @param options format options
   */
  public ServiceResponse convert(Model m,
                                 OutputFormat format,
                                 Map<String, String> options) {
    if (m == null || m.getObjects().isEmpty()) {
      //build an empty data response
      DataResponse r = new DataResponse();
      r.setFormat(format);
      return r;
    }

    // otherwise, convert, return a new DataResponse
    // (can contain up to ~ 1Gb unicode string data)
    // a TMP File is used instead of a byte array; set the file path as dataResponse.data value
    Path tmpPath = null;
    try {
      tmpPath = Files.createTempFile("cpath2", format.getExt());
      tmpPath.toFile().deleteOnExit();//to make sure...
      convert(m, format, options, Files.newOutputStream(tmpPath)); //OS gets closed there for sure.
      DataResponse dataResponse = new DataResponse();
      dataResponse.setFormat(format);
      dataResponse.setData(tmpPath);
      // extract and save data provider names
      dataResponse.setProviders(providers(m));
      return dataResponse;
    } catch (Exception e) {
      if(tmpPath != null) {
        try {
          Files.delete(tmpPath);
        } catch (Exception ex) {
          log.error(e.toString());
        }
      }
      return new ErrorResponse(INTERNAL_ERROR, e);
    }
  }


  /**
   * Converts a BioPAX Model to SBGN format.
   *
   * @param m         BioPAX object model to convert
   * @param stream    output stream for the SBGN-ML result
   * @param blackList skip-list of ubiquitous small molecules
   * @param doLayout  whether to apply the default layout or not
   * @throws IOException when there is an output stream writing error
   */
  private void convertToSBGN(Model m, OutputStream stream, Blacklist blackList, boolean doLayout) {
    L3ToSBGNPDConverter converter = new L3ToSBGNPDConverter(new ListUbiqueDetector(
      (blackList != null) ? blackList.getListed() : Collections.emptySet()), null, doLayout);
    converter.writeSBGN(m, stream);
  }


  /**
   * Converts service results that contain
   * a not empty BioPAX Model to GSEA format.
   *
   * @param m       paxtools model
   * @param stream  output stream
   * @param options format options
   * @throws IOException when there is an output stream writing error
   */
  private void convertToGSEA(Model m, OutputStream stream, Map<String, String> options)
    throws IOException {
    String idType;
    if ((idType = options.get("db")) == null)
      idType = "hgnc symbol";


    // It won't traverse into sub-pathways; will use only pre-defined organisms.
    // GSEAConverter's 'skipSubPathways' option is a different beast from the PC web api's 'subpw':
    // given sub-model (no matter how it was cut from the main model), there is still choice
    // to include gene IDs from sub-pathways (if there're any) into parent pathway's record or not.
    GSEAConverter gseaConverter = new GSEAConverter(idType, true, true);

    if (options.containsKey("organisms")) {
      Set<String> allowedTaxIds = Arrays.stream(options.get("organisms").split(","))
        .collect(Collectors.toSet());
      gseaConverter.setAllowedOrganisms(allowedTaxIds);
    }

    gseaConverter.setSkipOutsidePathways(false); //- because all Pathway objects were intentionally removed
    // before a get/graph query result gets here to be converted.
    gseaConverter.writeToGSEA(m, stream);
  }


  /*
   * Converts a not empty BioPAX Model (contained in the service bean)
   * to the SIF or <strong>single-file</strong> extended SIF format.
   * This is mainly for calling internally through the web service api.
   */
  private void convertToSIF(Model m, OutputStream out,
                            boolean extended, Map<String, String> options) {
    String db;
    if ((db = options.get("db")) == null) {
      db = "hgnc symbol"; //default
    }

    ConfigurableIDFetcher idFetcher = new ConfigurableIDFetcher();
    idFetcher.chemDbStartsWithOrEquals("chebi");

    if (db == null || db.isEmpty() || db.toLowerCase().startsWith("hgnc")) {
      idFetcher.seqDbStartsWithOrEquals("hgnc");
    } else if (db.toLowerCase().startsWith("uniprot")) {
      idFetcher.seqDbStartsWithOrEquals("uniprot");
    } else {
      idFetcher.seqDbStartsWithOrEquals(db);
    }

    SIFType[] sifTypes;
    if (options.containsKey("pattern")) {
      String[] sifNames = options.get("pattern").split(",");
      sifTypes = new SIFType[sifNames.length];
      int i = 0;
      for (String t : sifNames) {
        SIFEnum p = SIFEnum.typeOf(t);
        if(p != null) sifTypes[i++] = p;
      }
    } else {
      //default: apply all SIF rules but neighbor_of
      Collection<SIFType> c = new HashSet<>(Arrays.asList(SIFEnum.values()));
      c.remove(SIFEnum.NEIGHBOR_OF); //exclude NEIGHBOR_OF
      sifTypes = c.toArray(new SIFType[c.size()]);
    }

    SIFSearcher searcher = new SIFSearcher(idFetcher, sifTypes);
    searcher.setBlacklist(blacklist);

    if (extended) {
      Set<SIFInteraction> binaryInts = searcher.searchSIF(m);
      ExtendedSIFWriter.write(binaryInts, out);
    } else {
      searcher.searchSIF(m, out);
    }
  }

  /**
   * The list of datasources (data providers)
   * the BioPAX model contains.
   *
   * @param m BioPAX object model
   */
  @SuppressWarnings("unchecked")
  private Set<String> providers(Model m) {
    Set<String> names = null;

    if (m != null) {
      Collection<Provenance> provs = m.getObjects(Provenance.class);
      if (provs != null && !provs.isEmpty()) {
        names = new TreeSet<>();
        for (Provenance prov : provs) {
          String name = prov.getStandardName();
          if (name != null)
            names.add(name);
          else {
            name = prov.getDisplayName();
            if (name != null)
              names.add(name);
            else
              log.warn("No standard|display name found for " + prov);
          }
        }
      }
    }

    return (names != null && !names.isEmpty())
      ? names : Collections.EMPTY_SET;
  }
}
