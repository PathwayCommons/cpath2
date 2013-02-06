// $Id$
//------------------------------------------------------------------------------
/** Copyright (c) 2010 Memorial Sloan-Kettering Cancer Center.
 **
 ** This library is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** any later version.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** Memorial Sloan-Kettering Cancer Center
 ** has no obligations to provide maintenance, support,
 ** updates, enhancements or modifications.  In no event shall
 ** Memorial Sloan-Kettering Cancer Center
 ** be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** Memorial Sloan-Kettering Cancer Center
 ** has been advised of the possibility of such damage.  See
 ** the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this library; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 **/
package cpath.admin;

import cpath.config.CPathSettings;
import cpath.dao.*;
import cpath.dao.internal.DataServicesFactoryBean;
import cpath.importer.Fetcher;
import cpath.importer.Merger;
import cpath.importer.Premerge;
import cpath.importer.internal.ImportFactory;
import cpath.service.CPathService;
import cpath.service.OutputFormat;
import cpath.service.OutputFormatConverter;
import cpath.service.internal.OutputFormatConverterImpl;
import cpath.service.jaxb.*;
import cpath.warehouse.MetadataDAO;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.PathwayData;

import org.biopax.paxtools.io.*;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.biopax.validator.api.Validator;
import org.biopax.validator.api.beans.ValidatorResponse;
import org.biopax.validator.api.ValidatorUtils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import com.mchange.util.AssertException;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;


import static cpath.config.CPathSettings.*;
import static cpath.service.OutputFormat.*;

/**
 * Class which provides command line admin capabilities.
 * 
 * TODO (not urgent) use Apache CLI or alike lib...
 */
public class Admin implements Runnable {
	private static final Log LOG = LogFactory.getLog(Admin.class);

    private static Integer DEGREE_THRESHOLD = 100;
    private static Integer CONTROL_DEGREE_THRESHOLD = 6;

    // COMMAND Enum
    public static enum COMMAND {

        // command types
    	CREATE_TABLES("-create-tables"),
    	CREATE_INDEX("-create-index"),
        FETCH_METADATA("-fetch-metadata"),
		FETCH_DATA("-fetch-data"),
		PREMERGE("-premerge"),
		MERGE("-merge"),
		EXPORT("-export"),
		EXPORT_VALIDATION("-export-validation"),
        CREATE_BLACKLIST("-create-blacklist"),
        CONVERT("-convert"),
        CREATE_DOWNLOADS("-create-downloads"),
		;

        // string ref for readable name
        private String command;
        
        // contructor
        COMMAND(String command) { this.command = command; }

        // method to get enum readable name
        public String toString() { return command; }
    }
    
    // command, command parameter
    private COMMAND command;
    private String[] commandParameters;
   
    /**
     * Helper function to parse args.
     *
     * @param args String[]
     */
    private void parseArgs(final String[] args) {

        boolean validArgs = true;
        
		if (args.length > 5) { //max no. args (5th is reserved for future use)
			validArgs = false;
			return;
		}
 
        if(args[0].equals(COMMAND.CREATE_TABLES.toString())) {
        	this.command = COMMAND.CREATE_TABLES;
			if (args.length > 1) {
				// agrs[1] contains comma-separated db names
				this.commandParameters = args[1].split(",");
			} 
        } 
        else if(args[0].equals(COMMAND.CREATE_INDEX.toString())) {
			this.command = COMMAND.CREATE_INDEX;
        } 
        else if (args[0].equals(COMMAND.FETCH_METADATA.toString())) {
			if (args.length == 1) {
				validArgs = false;
			} else {
				this.command = COMMAND.FETCH_METADATA;
				this.commandParameters = new String[] {args[1]};
			}
        }
		else if (args[0].equals(COMMAND.FETCH_DATA.toString())) {
			this.command = COMMAND.FETCH_DATA;
			if (args.length >= 2) 
				this.commandParameters = new String[] {args[1]};
			else // command without extra parameter
				this.commandParameters = new String[] {null};
		}
		else if (args[0].equals(COMMAND.PREMERGE.toString())) {
			this.command = COMMAND.PREMERGE;
			this.commandParameters = new String[] { null, "false" };
			int j = 0;
			for (int i = 1; i < args.length && i < 3; i++) {
				if ("--usedatabases".equalsIgnoreCase(args[i])) {
					this.commandParameters[1] = "true";
					break; // flag is always the last arg.
				} else {
					this.commandParameters[j++] = args[i];
				}
			}
		}
		else if (args[0].equals(COMMAND.MERGE.toString())) {
			this.command = COMMAND.MERGE;
			this.commandParameters = new String[] { null, "false" };
			int j = 0;
			for (int i = 1; i < args.length && i < 3; i++) {
				if ("--force".equalsIgnoreCase(args[i])) {
					this.commandParameters[1] = "true";
					break; // flag is always the last arg.
				} else {
					this.commandParameters[j++] = args[i];
				}
			}
		}
		else if(args[0].equals(COMMAND.EXPORT.toString())) {
			if (args.length < 4) {
				validArgs = false;
			} else {
				this.command = COMMAND.EXPORT;
				this.commandParameters = new String[] {args[1], args[2], args[3]};
			} 
        } 
		else if(args[0].equals(COMMAND.EXPORT_VALIDATION.toString())) {
			if (args.length < 3) {
				validArgs = false;
			} else {
				this.command = COMMAND.EXPORT_VALIDATION;
				this.commandParameters = new String[] {args[1], args[2]};
			} 
        } 
		else if(args[0].equals(COMMAND.CREATE_BLACKLIST.toString())) {
            this.command = COMMAND.CREATE_BLACKLIST;
        } 
		else if(args[0].equals(COMMAND.CONVERT.toString())) {
			if (args.length < 4) {
				validArgs = false;
			} else {
				this.command = COMMAND.CONVERT;
				this.commandParameters = new String[] {args[1], args[2], args[3]};
			} 
        } 
		else if(args[0].equals(COMMAND.CREATE_DOWNLOADS.toString())) {
        	this.command = COMMAND.CREATE_DOWNLOADS;
        	if(args.length > 1) { //optional list of taxonomy ids
        		this.commandParameters = args[1].split(",");
            } 
        } 
		else {
            validArgs = false;
        }

        if (!validArgs) {
            throw new IllegalArgumentException("Invalid command: " + 
            		Arrays.toString(args));
        }
    }


	/**
     * Executes cPath^2 admin command.
     * 
     * At this point, 'command' and 'commandParameters' (array) 
     * have been already (re-)set
     * by previous {@link #parseArgs(String[])} method call
     * 
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        try {
            switch (command) {
            
            case CREATE_TABLES:
            	if(commandParameters != null) {
            		for(String db : commandParameters) {
            			String dbName = db.trim();
            			DataServicesFactoryBean.createSchema(dbName);
            		}
            	} else { // create all (as specified in the current cpath.properties)
            		DataServicesFactoryBean.createSchema(CPathSettings.get(CPath2Property.METADATA_DB));
            		DataServicesFactoryBean.createSchema(CPathSettings.get(CPath2Property.WAREHOUSE_DB));
            		DataServicesFactoryBean.createSchema(CPathSettings.get(CPath2Property.MAIN_DB));
            	}
            	break;
            case CREATE_INDEX:
           		// re-build the full-text index
           		// it gets the DB name from the environment variables (set in cpath.properties)
         		ApplicationContext ctx = null;
                ctx = new ClassPathXmlApplicationContext("classpath:applicationContext-cpathDAO.xml");
             	((PaxtoolsDAO)ctx.getBean("paxtoolsDAO")).index();
// Currently, we do not full-text search in the Warehouse
//              ctx = new ClassPathXmlApplicationContext("classpath:applicationContext-Warehouse.xml");
//           	((PaxtoolsDAO)ctx.getBean("warehouseDAO")).index();
            	break;
            case FETCH_METADATA:
                fetchMetadata(commandParameters[0]);
				break;
			case FETCH_DATA:
				fetchData(commandParameters[0]);
				break;
			case PREMERGE:
				runPremerge(commandParameters[0], commandParameters[1], 
					Boolean.parseBoolean(commandParameters[2]));
				break;
			case MERGE:
				runMerge(commandParameters[0], commandParameters[1], 
					Boolean.parseBoolean(commandParameters[2]));
				break;
            case EXPORT:
            	OutputStream os = new FileOutputStream(commandParameters[2]);
            	if("--all".equalsIgnoreCase(commandParameters[1]))
            		exportData(commandParameters[0], os);
            	else
            		exportData(commandParameters[0], os, commandParameters[1].split(","));
				break;
            case EXPORT_VALIDATION:
            	String outf = commandParameters[1];
            	os = new FileOutputStream(outf);
            	if(outf.endsWith(".html"))
            		exportValidation(commandParameters[0], os, true);
            	else
            		exportValidation(commandParameters[0], os, false);
            	
				break;
            case CREATE_BLACKLIST:
                createBlacklist();
                break;
            case CONVERT:
                OutputStream fos = new FileOutputStream(commandParameters[1]);
                OutputFormat outputFormat = OutputFormat.valueOf(commandParameters[2]);
                Resource blacklist = new DefaultResourceLoader().getResource("");
                OutputFormatConverter cnv = new OutputFormatConverterImpl(blacklist);
                convert(commandParameters[0], outputFormat, fos, cnv);
                break;
            case CREATE_DOWNLOADS:
                createDownloads(commandParameters);
                break;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

	/*
        Algorithm:
          * Get all SmallMoleculeReferences
          * Calculate the degrees (i.e. num of reactions and num of complexes it is associated with)
          * if it is bigger than the overall threshold and lower than the regulation threshold
          *     add it (and its members/entities/member entities to the list)
     */
    private void createBlacklist() throws IOException {
    	
    	final String src = CPathSettings.get(CPath2Property.MAIN_DB);
        LOG.debug("Creating blacklist from the " +  src + " database.");
        
        OutputStream output = new FileOutputStream(CPathSettings.getHomeDir() 
        	+ File.separator + "blacklist.txt");
        PaxtoolsDAO paxtoolsDAO = ImportFactory.buildPaxtoolsHibernateDAO(src); //- no indexes (no full-text search) here

        paxtoolsDAO.runAnalysis(new Analysis() {
            @Override
            public Set<BioPAXElement> execute(Model model, Object... args) {
                // This is to keep track of ids and to prevent multiple addition of a single element
                Set<BioPAXElement> blacklistedBPEs = new HashSet<BioPAXElement>();

                // Get all small molecule references
                for (SmallMoleculeReference ref : model.getObjects(SmallMoleculeReference.class)) {
                    Set<EntityReference> refs = new HashSet<EntityReference>();
                    // Collect its member refs (and their members refs if present)
                    getAllMemberRefs(ref, refs);

                    Set<PhysicalEntity> entities = new HashSet<PhysicalEntity>();
                    for (EntityReference entityReference : refs) {
                        for (SimplePhysicalEntity entity : entityReference.getEntityReferenceOf()) {
                            //  Pool all entities and their member entities
                            getAllSPEs(entity, entities);
                        }
                    }

                    // Count the degrees of the entities and sum them all
                    int regDegree = 0;
                    int allDegree = 0;
                    for (PhysicalEntity entity : entities) {
                        // These are the complexes
                        allDegree += entity.getComponentOf().size();

                        // These are the interactions
                        for (Interaction interaction : entity.getParticipantOf()) {
                            if(!(interaction instanceof ComplexAssembly)) { // Since we already count the complexes
                                allDegree++;

                                // Also count the control iteractions
                                if(interaction instanceof Control) {
                                    regDegree++;
                                }
                            }
                        }
                    } // End of iteration, degree calculation

                    // See if it needs to be blacklisted
                    if(regDegree < CONTROL_DEGREE_THRESHOLD && allDegree > DEGREE_THRESHOLD) {
                        LOG.debug("Adding " + ref.getDisplayName()
                                + " to the blacklist (Degrees: " + allDegree + ":" + regDegree + ")");
                        for (EntityReference entityReference : refs)
                            blacklistedBPEs.add(entityReference);

                        for (PhysicalEntity entity : entities)
                            blacklistedBPEs.add(entity);
                    }

                }

                // Write all the blacklisted ids to the output
                OutputStream output = (OutputStream) args[0];
                PrintStream printStream = new PrintStream(output);
                for (BioPAXElement bpe : blacklistedBPEs)
                    printStream.println(bpe.getRDFId());
                printStream.close();

                // we don't need it right now, but might become handy if we wanna extract the analysis later on
                return blacklistedBPEs;
            }

            private void getAllSPEs(PhysicalEntity entity, Set<PhysicalEntity> entities) {
                entities.add(entity);

                for(PhysicalEntity memberEntity : entity.getMemberPhysicalEntity()) {
                    if(!entities.contains(memberEntity)) {
                        entities.add(memberEntity);
                        getAllSPEs(memberEntity, entities);
                    }
                }
            }

            private void getAllMemberRefs(EntityReference ref, Set<EntityReference> refs) {
                refs.add(ref);

                for (EntityReference entityReference : ref.getMemberEntityReference()) {
                    if(!refs.contains(entityReference)) {
                        refs.add(entityReference);
                        getAllMemberRefs(entityReference, refs);
                    }
                }
            }

        }, output);
    }

    private void runMerge(String provider, String version, boolean force) {
		// pc dao
		ApplicationContext context = new ClassPathXmlApplicationContext("classpath:applicationContext-cpathDAO.xml");
		final PaxtoolsDAO pcDAO = (PaxtoolsDAO)context.getBean("paxtoolsDAO");

		LOG.info("runMerge: provider=" + provider + "; version=" + version
			+ "; --force=" + force);
		
		Merger merger = (provider == null || provider.isEmpty())
				? ImportFactory.newMerger(pcDAO, force)
				: ImportFactory.newMerger(pcDAO, provider, version, force);
		merger.merge();
	}

	
	private void runPremerge(String provider, String version, boolean createPremergeDbs) {
		ApplicationContext context =
            new ClassPathXmlApplicationContext(new String [] { 	
            		"classpath:applicationContext-Metadata.xml", 
            		"classpath:applicationContext-Warehouse.xml",
            		"classpath:applicationContext-biopaxValidation.xml", 
					"classpath:applicationContext-cvRepository.xml"});
		MetadataDAO metadataDAO = (MetadataDAO) context.getBean("metadataDAO");
		PaxtoolsDAO warehouseDAO = (PaxtoolsDAO) context.getBean("warehouseDAO");
		Validator validator = (Validator) context.getBean("validator");
        Premerge premerge = ImportFactory.newPremerge(metadataDAO, warehouseDAO, validator, provider);
        LOG.info("runPremerge: provider=" + provider + "; version=" + version
    			+ "; DBs=" + createPremergeDbs);
        premerge.premerge();
	}

	
	/**
     * Helper function to get provider metadata.
     *
     * @param location String URL or local file.
     * @throws IOException
     */
    private void fetchMetadata(final String location) throws IOException {
        ApplicationContext context =
            new ClassPathXmlApplicationContext(new String [] { 	
            	"classpath:applicationContext-Metadata.xml"});
        MetadataDAO metadataDAO = (MetadataDAO) context.getBean("metadataDAO");
        Fetcher fetcher = ImportFactory.newFetcher(false);
    	
        // grab the data
        Collection<Metadata> metadata = fetcher.readMetadata(location);
        
        // process metadata
        for (Metadata mdata : metadata) {
            metadataDAO.importMetadata(mdata);
        }
    }


    /**
     * Downloads pathway or warehouse (protein, small molecule) data
     * from the location specified in the metadata.
	 *
     * @param provider String
     * @throws IOException
     */
    private void fetchData(final String provider) throws IOException {
		ApplicationContext context =
            new ClassPathXmlApplicationContext(new String [] { 	
            		"classpath:applicationContext-Metadata.xml"});
        MetadataDAO metadataDAO = (MetadataDAO) context.getBean("metadataDAO");
        Fetcher fetcher = ImportFactory.newFetcher(true);
    	
		// get metadata
		Collection<Metadata> metadataCollection = getMetadata(metadataDAO, provider);
		// sanity check
		if (metadataCollection == null || metadataCollection.isEmpty()) {
			LOG.error("Unknown provider identifier: " + provider);
			return;
		}
		
		// interate over all metadata
		for (Metadata metadata : metadataCollection) {			
			try {
				fetcher.fetchData(metadata); 
			} catch (Exception e) {
				LOG.error("Failed fetching data for " + metadata.toString() 
					+ ". Skipping...", e);
				continue;
			}
			
			LOG.info("FETCHING DONE : " + metadata.getIdentifier()
					+ "." + metadata.getVersion());
		}
	}

    
	/**
	 * Given a provider, returns a collection of Metadata.
	 *
	 * @param provider String
	 * @return Collection<Metadata>
	 */
	private Collection<Metadata> getMetadata(final MetadataDAO metadataDAO, final String provider) 
	{
		Collection<Metadata> toReturn = new HashSet<Metadata>();
		if (provider == null || provider.isEmpty()) {
			toReturn = metadataDAO.getAllMetadata();
		} else {
			Metadata md = metadataDAO.getMetadataByIdentifier(provider);
			if(md != null)
				toReturn.add(md);
		}
		return toReturn;
	}

	
	public static void exportData(final String src,  final OutputStream output, String... uris) 
		throws IOException
	{		
		Integer pk = null; // aka pathway_id
		try {
			pk = Integer.valueOf(src);
			if(LOG.isDebugEnabled())
				LOG.debug("Export from the original data," +
					" pathway_id=" + src);
		} catch (NumberFormatException e) {
			if(LOG.isDebugEnabled())
				LOG.debug("Export from the database: " + src);
		}
		/* if the first argument was not a (integer) pathway_id from pathwayData,
		 * it must be a PaxtoolsDAO db name (i.e,, a premerge or cpath2 main one)
		 */
		if(pk == null) {
			PaxtoolsDAO dao = ImportFactory.buildPaxtoolsHibernateDAO(src); 
			// - this instantiation method works with any cpath2 db, but full-text search will be disabled here (we do not need it now)
			dao.exportModel(output, uris);
		} else {
			// get pathwayData (bean)
			PathwayData pdata = getPathwayData(pk);
			if(pdata != null) {
				// get premergeData (OWL text)
				byte[] data = pdata.getPremergeData();
				if (data != null && data.length > 0) {
					if (uris.length > 0) { // extract a sub-model
						SimpleIOHandler handler = new SimpleIOHandler(); // auto-detect Level
						Model model = handler.convertFromOWL(new ByteArrayInputStream(data));
						//handler.setFactory(model.getLevel().getDefaultFactory());
						handler.convertToOWL(model, output, uris);
					} else { 
						/*	write all (premergeData -
							cleaned/converted/validated/normalized from
							the original provider's file )	*/
						output.write(data);
						output.flush();
					}
				}
				else {
					// no data found
					LOG.error("Data not found! Have you run '-premerge' for "
						+ pdata + "?");
				}
			} 
			else {
				LOG.error("Record not found: pathway_id=" + pk);
			}
		}
	}	
	
	
	
	/**
	 * Exports from the PathwayData entity(-ies) and 
	 * writes the BioPAX validation report (as XML or HTML).
	 * 
	 * @param key an existing cpath2 pathwayData.pathway_id or metadata.identifier (provider).
	 * @param out output stream
	 * @param asHtml write as HTML report (transform from the XML)
	 *
	 */
	static void exportValidation(final String key, final OutputStream out, boolean asHtml) 
		throws IOException 
	{
		ApplicationContext context =
	            new ClassPathXmlApplicationContext(new String [] { 	
	            	"classpath:applicationContext-Metadata.xml"});
	    MetadataDAO metadataDAO = (MetadataDAO) context.getBean("metadataDAO");
	    
	    ValidatorResponse report = null;
		Integer pk = null;
		try {
			pk = Integer.valueOf(key);
		} catch (NumberFormatException e) {}
		
		if(pk != null) {
			PathwayData pathwayData = getPathwayData(pk);
			if (pathwayData != null) {
				if(LOG.isInfoEnabled())
		    		LOG.info("Getting validation report for pathway_id: " + pk 
		    			+ " (" + pathwayData.getIdentifier() + ") "
		    			+ "...");
				report = metadataDAO.getValidationReport(pk);
			} else {
				if(LOG.isInfoEnabled())
		    		LOG.info("Getting validation report: pathway_id: " + pk + " does not exist.");
				System.err.println("Getting validation report for pathway_id=" + pk + ": not found.");
				return;
			}
		} else {
			if(LOG.isInfoEnabled())
	    		LOG.info("Getting validation report for data source: " + key + "...");
			report = metadataDAO.getValidationReport(key);
		}

    	if(report == null) {
    		System.err.println("No validation report found or error.");
    	} else {
    		OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");
    		Source xsl = (asHtml) 
    			? new StreamSource((new DefaultResourceLoader())
    					.getResource("classpath:html-result.xsl").getInputStream())
    			: null;

			ValidatorUtils.write(report, writer, xsl); 
    		writer.flush();
    	}
	}	

	
	private static PathwayData getPathwayData(final Integer pk) {
		ApplicationContext ctx = new ClassPathXmlApplicationContext(
				"classpath:applicationContext-Metadata.xml");
		MetadataDAO dao = (MetadataDAO) ctx.getBean("metadataDAO");
		return dao.getPathwayData(pk);
	}
	
		
	private static String usage() 
	{
		StringBuilder toReturn = new StringBuilder();
		toReturn.append("Usage: <-command_name> <command_args...>" + NEWLINE);
		toReturn.append("commands:" + NEWLINE);
		// data import (instance creation) pipeline :
		toReturn.append(COMMAND.CREATE_TABLES.toString() + " [<table1,table2,..>]" + NEWLINE);
		toReturn.append(COMMAND.FETCH_METADATA.toString() + " <url>" + NEWLINE);
		toReturn.append(COMMAND.FETCH_DATA.toString() + " <metadataId or --all>" + NEWLINE);
		toReturn.append(COMMAND.PREMERGE.toString() + " [<metadataId>] [--usedatabases]" + NEWLINE);
		toReturn.append(COMMAND.MERGE.toString() + " [<metadataId>] [--force]"+ NEWLINE);
		toReturn.append(COMMAND.CREATE_INDEX.toString() + NEWLINE);
        toReturn.append(COMMAND.CREATE_BLACKLIST.toString() + " (creates blacklist.txt in the cpath2 home directory)" + NEWLINE);
        toReturn.append(COMMAND.CREATE_DOWNLOADS.toString() + "[<taxonomy_id|name,taxonomy_id|name,..>] (better use standard names, "
        	+"e.g., (exactly) as \"homo sapiens,mus musculus\", because generated file names will look more user-friendly)"  + NEWLINE);        
        // other useful (utility) commands
		toReturn.append(COMMAND.EXPORT.toString() + " <dbName or pathway_id> <uri,uri,.. or --all> <outfile>" +
			" (dbName - any supported by PaxtoolsDAO DB; pathway_id is a PK of the pathwayData table - to extract 'premerged' data)" + NEWLINE);
		toReturn.append(COMMAND.EXPORT_VALIDATION.toString() 
			+ " <provider>|<pathway_id> <output_file[.xml|.html]> (<provider> - metadata identifier or <pathway_id> - see above; "
			+ "output_file will contain the Validator Response XML unless '.html' file extension is used, " +
			"in wich case the XML is there auto-transformed to offline HTML+Javascript content)" + NEWLINE);
		toReturn.append(COMMAND.CONVERT.toString() + " <biopax-file(.owl|.gz)> <output-file> <output format>" + NEWLINE);

		return toReturn.toString();
	}

    /**
     * The big deal main.
     * 
     * @param args String[]
     */    
    public static void main(String[] args) throws Exception {
    	LOG.debug("Command-line arguments were: " + Arrays.toString(args));
    	
    	// sanity check
        if (args.length == 0) {
            System.err.println("Missing args to Admin.");
			System.err.println(Admin.usage());
            System.exit(-1);
        }
    	
    	// "CPATH2_HOME" env. var. must be set
        String home = System.getenv(HOME_VARIABLE_NAME);
    	if (home==null) {
            System.err.println("Please set " + HOME_VARIABLE_NAME 
            	+ " environment variable " +
            	" (point to a directory where cpath.properties, etc. files are placed)");
            System.exit(-1);
    	}
    	// the JVM option must be set to the same value as well!
    	if (!home.equals(System.getProperty(HOME_VARIABLE_NAME))) {
            System.err.println("Please also set the java property " 
            	+ HOME_VARIABLE_NAME 
            	+ ", i.e., run with -D" + HOME_VARIABLE_NAME + "=" 
            	+ home + " option.");
            System.exit(-1);
    	}
    	
    	// configure logging
    	PropertyConfigurator.configure(home + File.separator + "log4j.properties");

    	// set JVM property to be used by other modules (in spring context)
    	System.setProperty(HOME_VARIABLE_NAME, home);

        // Extract blacklist values, if can't, then use the default values
        try {
            DEGREE_THRESHOLD = Integer.parseInt(get(CPath2Property.BLACKLIST_DEGREE_THRESHOLD));
            CONTROL_DEGREE_THRESHOLD = Integer.parseInt(get(CPath2Property.BLACKLIST_CONTROL_THRESHOLD));
        } catch (NumberFormatException e) {
            LOG.warn("Could not found the properties " + CPath2Property.BLACKLIST_CONTROL_THRESHOLD + " and "
                    + CPath2Property.BLACKLIST_DEGREE_THRESHOLD + " in the properties file. Using default values: "
                    + CONTROL_DEGREE_THRESHOLD + " and " + DEGREE_THRESHOLD + "."
            );
        }

    	if(!Charset.defaultCharset().equals(Charset.forName("UTF-8")))
    		if(LOG.isWarnEnabled())
    			LOG.warn("Default Charset, " + Charset.defaultCharset() 
    				+ " (is NOT 'UTF-8'...)");
    	
    	// create the TMP dir inside the home dir if it does not exist yet
		File dir = new File(CPathSettings.localDataDir());
		if(!dir.exists()) {
			dir.mkdir();
		}
    	
    	
		Admin admin = new Admin();
		try {
			admin.parseArgs(args);
			admin.run();
		} catch (Exception e) {
			System.err.println(e + NEWLINE + usage());
		}
		// required because MySQL Statement Cancellation Timer thread is still running
		System.exit(0);
    }
    	
	
	private static void convert(String biopaxFile, OutputFormat outputFormat, 
			OutputStream output, OutputFormatConverter formatConverter) throws IOException 
	{
		InputStream is = biopaxStream(biopaxFile);
		ServiceResponse res = formatConverter.convert(is, outputFormat);
		if (res instanceof ErrorResponse) {
			System.err.println(res.toString());
		} else {
			String data = (String) ((DataResponse)res).getData();
			output.write(data.getBytes("UTF-8"));
			output.flush();
		}
	}
	
	
	private static void convertToExtSif(String biopaxFile, OutputFormatConverter formatConverter,
			OutputStream edgeStream, OutputStream nodeStream) throws IOException 
	{
		InputStream is = biopaxStream(biopaxFile);		
		Model m = (new SimpleIOHandler()).convertFromOWL(is);
		formatConverter.convertToExtendedBinarySIF(m, edgeStream, nodeStream);
	}

	
    private static InputStream biopaxStream(String biopaxFile) throws IOException {
		return (biopaxFile.endsWith(".gz"))
			? new GZIPInputStream(new FileInputStream(biopaxFile)) 
			: new FileInputStream(biopaxFile);
	}

	private static void createDownloads(String[] organisms) 
    		throws IOException
    {	
    	// create the TMP dir inside the home dir if it does not exist yet
		File f = new File(CPathSettings.getHomeDir() + File.separator + DOWNLOADS_SUBDIR);
		if(!f.exists()) 
			f.mkdir();
    	
		ApplicationContext context = new ClassPathXmlApplicationContext(
				new String [] {
					"classpath:applicationContext-cpathDAO.xml",
	            	"classpath:applicationContext-Metadata.xml",
	            	"classpath:applicationContext-cpathService.xml"
	            }
			);
		PaxtoolsDAO dao = (PaxtoolsDAO) context.getBean("paxtoolsDAO");
		OutputFormatConverter formatConverter = (OutputFormatConverter) context.getBean("outputFormatConverter");
		CPathService service = (CPathService) context.getBean("service");
		
    	// 1) export everything
		createArchives("all", dao, formatConverter, null, null);
    	
    	// 2) export by organism
        LOG.info("create-downloads: preparing data 'by organism' archives...");
        for(String org : organisms)
        	createArchives(org.toLowerCase(), dao, formatConverter, null, new String[]{org});
		
		// 3) export by datasource
        LOG.info("create-downloads: preparing 'by datasource' archives...");
        for(SearchHit ds : service.dataSources().getSearchHit())
        	createArchives(ds.getName().toLowerCase(), dao, formatConverter, new String[]{ds.getName()}, null);
	}


	private static Collection<String> findAllUris(PaxtoolsDAO db, 
    		Class<? extends BioPAXElement> type, String[] ds, String[] org) 
    {
    	Collection<String> uris = new ArrayList<String>();
    	
    	// using PaxtoolsDAO (no service-tier cache) instead CPathService here
    	SearchResponse resp = db.search("*", 0, type, ds, org);
    	int page = 0;
		while(!resp.isEmpty()) {
			for(SearchHit h : resp.getSearchHit())
				uris.add(h.getUri());
			//next page
			resp = db.search("*", ++page, type, ds, org);
		}
    	
    	return uris;
    }
    

    private static void createArchives(String filePrefix, PaxtoolsDAO dao, 
    	OutputFormatConverter formatConverter, String[] datasources, String[] organisms) 
    	throws IOException 
    {
    	// grab the BioPAX first -
        final String biopaxDataArchive = CPathSettings.getHomeDir() + File.separator 
        		+ DOWNLOADS_SUBDIR + File.separator 
        		+ CPathSettings.get(CPath2Property.PROVIDER) + " " 
        		+ filePrefix + ".BIOPAX.owl.gz";
        
        // check file exists
        if(!(new File(biopaxDataArchive)).exists()) {	
        	Collection<String> uris = new HashSet<String>();
        	
        	if(organisms != null || datasources != null) {
        		//find all pathways and interactions only (all child elements will be then exported too)
        		uris.addAll(findAllUris(dao, Pathway.class, datasources, organisms));
        		uris.addAll(findAllUris(dao, Interaction.class, datasources, organisms));
        	}
        	
        	// save entire data, compressed, in several formats
        	LOG.info("create-downloads: preparing '" + 	biopaxDataArchive);
        	//it is important to use dao.exportModel(out) and not service.fetchBiopaxModel(),
        	// because the latter w/o args will return empty result, whereas the former - ALL data.
        	dao.exportModel(new GZIPOutputStream(
        		new FileOutputStream(biopaxDataArchive)), uris.toArray(new String[]{}));
        } else
        	LOG.info(biopaxDataArchive + " already exists; skip creating it " +
        		"again (delete existing files if you want to start over)");

        //quickly test whether there will be pathways (to then skip exporting to GSEA)
        SearchResponse resp = dao.search("*", 0, Pathway.class, datasources, organisms);
        boolean hasPathways = (resp.getNumHits() > 0);
        OutputFormat[] formats = (hasPathways) 
        	? new OutputFormat[]{BINARY_SIF, EXTENDED_BINARY_SIF, GSEA} 
        	: new OutputFormat[]{BINARY_SIF, EXTENDED_BINARY_SIF};
        
		for(OutputFormat outf : formats)
			createArchive(biopaxDataArchive, outf, filePrefix, formatConverter);
	}
	
	private static void createArchive(String biopaxDataArchive, OutputFormat format, 
			String prefix, OutputFormatConverter formatConverter) throws IOException 
	{	
		if(format == OutputFormat.BIOPAX)
			throw new AssertException("Converting BioPAX to BioPAX");
	
		String archiveName = CPathSettings.getHomeDir() + File.separator 
				+ CPathSettings.DOWNLOADS_SUBDIR + File.separator 
				+ CPathSettings.get(CPath2Property.PROVIDER) + " " 
				+ prefix + "." + formatAndExt(format) + ".gz";
		LOG.info("create-downloads: generating " + archiveName);
		
		
        if(!(new File(archiveName)).exists()) {
    		//Extended SIF will be here split in two separate files (edges and nodes)
    		if(format == EXTENDED_BINARY_SIF) {
    			//write edges and nodes into separate archives
    			GZIPOutputStream edgeStream = new GZIPOutputStream(new FileOutputStream(archiveName));
    			GZIPOutputStream nodeStream = new GZIPOutputStream(new FileOutputStream(
    				CPathSettings.getHomeDir() + File.separator 
    					+ CPathSettings.DOWNLOADS_SUBDIR + File.separator 
    					+ CPathSettings.get(CPath2Property.PROVIDER) + " " 
    					+ prefix + "." + format + ".nodes.tsv.gz"));
    		
    			convertToExtSif(biopaxDataArchive, formatConverter, edgeStream, nodeStream);
    			
    			IOUtils.closeQuietly(edgeStream);
    			IOUtils.closeQuietly(edgeStream);
    		} else {    	
    			GZIPOutputStream zos = new GZIPOutputStream(new FileOutputStream(archiveName));
    			convert(biopaxDataArchive, format, zos, formatConverter);
        		IOUtils.closeQuietly(zos);
    		}
        } else
        	LOG.info(archiveName + " exists; skip creating it " +
            	"(delete if you want to start over)");
	}

	private static String formatAndExt(OutputFormat format) {
		switch (format) {
		case BIOPAX:
			return format + ".owl";
		case GSEA:
			return format + ".gmt";
		case SBGN:
			return format + ".xml";
		default:
			return format + ".tsv";
		}
	}
}
