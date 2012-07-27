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

import cpath.dao.*;
import cpath.dao.internal.DataServicesFactoryBean;
import cpath.importer.Fetcher;
import cpath.importer.Merger;
import cpath.importer.Premerge;
import cpath.importer.internal.*;
import cpath.warehouse.MetadataDAO;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.PathwayData;

import org.biopax.paxtools.io.*;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.biopax.validator.Validator;
import org.biopax.validator.result.ValidatorResponse;
import org.biopax.validator.utils.BiopaxValidatorUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;


import static cpath.config.CPathSettings.*;

/**
 * Class which provides command line admin capabilities.
 * 
 * TODO (not urgent) re-factor to use JLine or Apache CLI
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
        CREATE_BLACKLIST("-create-blacklist")
		;

        // string ref for readable name
        private String command;
        
        // contructor
        COMMAND(String command) { this.command = command; }

        // method to get enum readable name
        public String toString() { return command; }
    }
    
    // INDEX_TYPE enum
    public static enum INDEX_TYPE {
        // cpath2 fulltext index types
    	MAIN,
    	PROTEINS,
    	MOLECULES,
    	METADATA;
    }

    // command, command parameter
    private COMMAND command;
    private String[] commandParameters;


    public void setCommandParameters(String[] args) {
		this.commandParameters = args;
        // parse args
        parseArgs(args);
	}
    
    /**
     * Helper function to parse args.
     *
     * @param args String[]
     */
    private void parseArgs(final String[] args) {

        boolean validArgs = true;

        // TODO: use gnu getopt or some variant  
        if(args[0].equals(COMMAND.CREATE_TABLES.toString())) {
			if (args.length != 2) {
				validArgs = false;
			} else {
				this.command = COMMAND.CREATE_TABLES;
				// agrs[1] contains comma-separated db names
				this.commandParameters = args[1].split(",");
			} 
        } 
        else if(args[0].equals(COMMAND.CREATE_INDEX.toString())) {
			if (args.length != 2) {
				validArgs = false;
			} else {
				this.command = COMMAND.CREATE_INDEX;
				// agrs[1] contains comma-separated db names
				this.commandParameters = new String[]{args[1].toUpperCase()};
			} 
        } 
        else if (args[0].equals(COMMAND.FETCH_METADATA.toString())) {
			if (args.length != 2) {
				validArgs = false;
			}
			else {
				this.command = COMMAND.FETCH_METADATA;
				this.commandParameters = new String[] { args[1] };
			}
        }
		else if (args[0].equals(COMMAND.FETCH_DATA.toString())) {
			if (args.length != 2) {
				validArgs = false;
			} else {
				this.command = COMMAND.FETCH_DATA;
				this.commandParameters = new String[] { args[1] };
			}
		}
		else if (args[0].equals(COMMAND.PREMERGE.toString())) {
			this.command = COMMAND.PREMERGE;
			validArgs = processOptionalArgs(this.command, args);
		}
		else if (args[0].equals(COMMAND.MERGE.toString())) {
			this.command = COMMAND.MERGE;
			validArgs = processOptionalArgs(this.command, args);
		}
		else if(args[0].equals(COMMAND.EXPORT.toString())) {
			if (args.length != 4) {
				validArgs = false;
			} else {
				this.command = COMMAND.EXPORT;
				this.commandParameters = new String[] {args[1], args[2], args[3]};
			} 
        } 
		else if(args[0].equals(COMMAND.EXPORT_VALIDATION.toString())) {
			if (args.length != 3) {
				validArgs = false;
			} else {
				this.command = COMMAND.EXPORT_VALIDATION;
				this.commandParameters = new String[] {args[1], args[2]};
			} 
        } else if(args[0].equals(COMMAND.CREATE_BLACKLIST.toString())) {
            if(args.length != 3) {
                validArgs = false;
            } else {
                this.command = COMMAND.CREATE_BLACKLIST;
                this.commandParameters = new String[] {args[1], args[2]};
            }
        } else {
            validArgs = false;
        }

        if (!validArgs) {
            throw new IllegalArgumentException("Invalid command passed to Admin.");
        }
    }

    
	private boolean  processOptionalArgs(COMMAND cmd, final String[] args) {
		//args[0],the command name, plus max. 4 optional parameters...
		
		if(cmd != COMMAND.PREMERGE && cmd != COMMAND.MERGE) {
			throw new UnsupportedOperationException(cmd + " is not supported!");
		}
		
		if (args.length > 5) {
			return false;
		}
		
		String usePremergeDbflag = "--usedatabases"; // not used with for 'merge' anymore
		String forceMergeFlag = "--force";
		
		// set default values first, i.e.,
		// do all data providers and versions, do not create/use 
		// the pre-merge DBs, merge valid BioPAX (no forcing)
		this.commandParameters = new String[] {null, null, "false", "false"};	
		for(int i=1 ; i < args.length ; i++) {
			if(usePremergeDbflag.equalsIgnoreCase(args[i])) {
				this.commandParameters[2] = "true";
			} else if(forceMergeFlag.equalsIgnoreCase(args[i])) {
				this.commandParameters[3] = "true";
			} else {
				this.commandParameters[i-1] = args[i];
			}
		}
		
		return true;
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
            			// create db schema and lucene index
            			DataServicesFactoryBean.createSchema(dbName);
            		}
            	}
            	break;
            case CREATE_INDEX:
            	if(commandParameters != null) {
            		// re-build the full-text index
            		// it gets the DB name from the environment variables (set in cpath.properties)
            		INDEX_TYPE mtype = INDEX_TYPE.valueOf(commandParameters[0]);
            		ApplicationContext ctx = null;
            		switch (mtype) {
                    case MOLECULES:
                    	//DataServicesFactoryBean.rebuildMoleculesIndex();
                    	ctx = new ClassPathXmlApplicationContext("classpath:applicationContext-whouseMolecules.xml");
                		((PaxtoolsDAO)ctx.getBean("moleculesDAO")).index();
                    	break;
                    case PROTEINS:
                		//DataServicesFactoryBean.rebuildProteinsIndex();
                    	ctx = new ClassPathXmlApplicationContext("classpath:applicationContext-whouseProteins.xml");
                		((PaxtoolsDAO)ctx.getBean("proteinsDAO")).index();
                    	break;
                    case MAIN :
                       	//DataServicesFactoryBean.rebuildMainIndex();
                    	ctx = new ClassPathXmlApplicationContext("classpath:applicationContext-cpathDAO.xml");
                		((PaxtoolsDAO)ctx.getBean("paxtoolsDAO")).index();
                       	
                    	break;
            		}
            	}
            	break;
            case FETCH_METADATA:
                fetchMetadata(commandParameters[0]);
				break;
			case FETCH_DATA:
				fetchWarehouseData(commandParameters[0]);
				break;
			case PREMERGE:
				runPremerge(commandParameters[0], commandParameters[1], 
					Boolean.parseBoolean(commandParameters[2]));
				break;
			case MERGE:
				runMerge(commandParameters[0], commandParameters[1], 
					Boolean.parseBoolean(commandParameters[3]));
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
            	Integer pk = Integer.valueOf(commandParameters[0]);
            	if(outf.endsWith(".html"))
            		exportValidation(pk, os, true);
            	else
            		exportValidation(pk, os, false);
            	
				break;
            case CREATE_BLACKLIST:
                os = new FileOutputStream(commandParameters[1]);
                createBlacklist(commandParameters[0], os);
                
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
    private void createBlacklist(final String src, OutputStream output) {
        LOG.debug("Creating blacklist from the database: " + src);
        PaxtoolsDAO paxtoolsDAO = ImportFactory.buildPaxtoolsHibernateDAO(src);

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
		
		Merger merger = (provider == null)
				? ImportFactory.newMerger(pcDAO, force)
				: ImportFactory.newMerger(pcDAO, provider, version, force);
		merger.merge();
	}

	
	private void runPremerge(String provider, String version, boolean createPremergeDbs) {
		ApplicationContext context =
            new ClassPathXmlApplicationContext(new String [] { 	
            		"classpath:applicationContext-whouseDAO.xml", 
            		"classpath:applicationContext-biopaxValidation.xml", 
					"classpath:applicationContext-cvRepository.xml"});
		MetadataDAO metadataDAO = (MetadataDAO) context.getBean("metadataDAO");
		Validator validator = (Validator) context.getBean("validator");
        Premerge premerge = ImportFactory.newPremerge(metadataDAO, validator, createPremergeDbs, provider, version);
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
            	"classpath:applicationContext-whouseDAO.xml"});
        MetadataDAO metadataDAO = (MetadataDAO) context.getBean("metadataDAO");
        Fetcher fetcher = ImportFactory.newFetcher();
    	
        // grab the data
        Collection<Metadata> metadata = fetcher.getMetadata(location);
        
        // process metadata
        for (Metadata mdata : metadata) {
            metadataDAO.importMetadata(mdata);
        }
    }


    /**
     * Helper function to get warehouse (protein, small molecule) data.
	 *
     * @param provider String
     * @throws IOException
     */
    private void fetchWarehouseData(final String provider) throws IOException {
		ApplicationContext context =
            new ClassPathXmlApplicationContext(new String [] { 	
            		"classpath:applicationContext-whouseDAO.xml"});
        MetadataDAO metadataDAO = (MetadataDAO) context.getBean("metadataDAO");
        Fetcher fetcher = ImportFactory.newFetcher();
    	
		// get metadata
		Collection<Metadata> metadataCollection = getMetadata(metadataDAO, provider);

		// sanity check
		if (metadataCollection == null || metadataCollection.isEmpty()) {
			LOG.error("Unknown provider identifier: " + provider);
			return;
		}

		// process small molecule references data
		ApplicationContext contextM = new ClassPathXmlApplicationContext(new String [] { 	
            		"classpath:applicationContext-whouseMolecules.xml"});
        PaxtoolsDAO smallMoleculesDAO = (PaxtoolsDAO) contextM.getBean("moleculesDAO");
        
        
        ApplicationContext contextP = new ClassPathXmlApplicationContext(new String [] {
		"classpath:applicationContext-whouseProteins.xml"});
		PaxtoolsDAO proteinsDAO = (PaxtoolsDAO) contextP.getBean("proteinsDAO");
		
		// interate over all metadata
		for (Metadata metadata : metadataCollection) {			
			try {
				/* it won't replace existing files (name: metadata.getLocalDataFile()),
				 * which allows for manual correction of re-importing of previously fetched data
				 */
				fetcher.fetchData(metadata); 
			} catch (Exception e) {
				LOG.error("Failed fetching data for " + metadata.toString() 
					+ ". Skipping...", e);
				continue;
			}
			
			if (!metadata.getType().isWarehouseData()) 
			{
					// collect pathway data versions of the same provider
					Collection<String> savedVersions = new HashSet<String>();
					for(PathwayData pd: metadataDAO
							.getPathwayDataByIdentifier(metadata.getIdentifier())) {
						savedVersions.add(pd.getVersion());
					}
					// lets not fetch the same version data
					if (!savedVersions.contains(metadata.getVersion())) {
						// grab the data
						Collection<PathwayData> pathwayData =
								fetcher.getProviderPathwayData(metadata);
	        
						// process pathway data
						for (PathwayData pwData : pathwayData) {
							metadataDAO.importPathwayData(pwData);
						}
					} else {
						if(LOG.isInfoEnabled())
							LOG.info("Skipping existing pathway data (- same identifier and version)");
					}
			} 
			else if (metadata.getType() == Metadata.TYPE.PROTEIN) 
			{
				// parse/save
				fetcher.storeWarehouseData(metadata, (Model)proteinsDAO);
        	} 
			else if (metadata.getType() == Metadata.TYPE.SMALL_MOLECULE) 
			{
		        // parse/save
				fetcher.storeWarehouseData(metadata, (Model)smallMoleculesDAO);
			} 
			else if (metadata.getType() == Metadata.TYPE.MAPPING) 
			{
				// already done (by fetcher.fetchData(metadata)).
			}
			
			if(LOG.isInfoEnabled())
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
		if (provider.equalsIgnoreCase("--all")) {
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
			PaxtoolsDAO pemergeDAO = ImportFactory.buildPaxtoolsHibernateDAO(src); // yes, it also works for the the main db
			pemergeDAO.exportModel(output, uris);
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
	 * Exports from the PathwayData entity bean and 
	 * writes the BioPAX validation report (as XML or HTML).
	 * 
	 * @param pk pathway_id in the cpath2 pathwayData table
	 * @param out output stream
	 * @param asHtml write as HTML report (transform from the XML)
	 *
	 */
	public static void exportValidation(final Integer pk, final OutputStream out, boolean asHtml) 
		throws IOException 
	{
		// get validationResults XML from PathwayData beans
		PathwayData pathwayData = getPathwayData(pk);
		if (pathwayData != null) {
			//FYI: use SET GLOBAL max_allowed_packet = 256000000; for the MySQL instance; otherwise it returns null for large enough reports!
			byte[] xmlResult = pathwayData.getValidationResults();
			if(!asHtml) {
				out.write(xmlResult);
				out.flush();
			} else {
				try { //converting to the HTML page
					//the xslt stylesheet exists in the biopax-validator-core module
					Source xsl = new StreamSource((new DefaultResourceLoader())
							.getResource("classpath:html-result.xsl").getInputStream());
					ValidatorResponse resp = (ValidatorResponse) BiopaxValidatorUtils.getUnmarshaller()
							.unmarshal(new BufferedInputStream(new ByteArrayInputStream(xmlResult)));
					Writer writer = new OutputStreamWriter(out, "UTF-8");
					BiopaxValidatorUtils.write(resp, writer, xsl); 
					writer.flush();
				} catch (Exception e) {
					LOG.error(e);
				}
			}
		}
	}	

	
	private static PathwayData getPathwayData(final Integer pk) {
		ApplicationContext ctx = new ClassPathXmlApplicationContext(
				"classpath:applicationContext-whouseDAO.xml");
		MetadataDAO dao = (MetadataDAO) ctx.getBean("metadataDAO");
		return dao.getPathwayData(pk);
	}
	
		
	private static String usage() 
	{
		StringBuilder toReturn = new StringBuilder();
		toReturn.append("Usage: <-command_name> <command_args...>" + NEWLINE);
		toReturn.append("commands:" + NEWLINE);
		toReturn.append(COMMAND.CREATE_TABLES.toString() + " <table1,table2,..>" + NEWLINE);
		toReturn.append(COMMAND.CREATE_INDEX.toString() + 
			" <type> (types are: proteins, molecules, main)" + NEWLINE);
		toReturn.append(COMMAND.FETCH_METADATA.toString() + " <url>" + NEWLINE);
		toReturn.append(COMMAND.FETCH_DATA.toString() + " <metadataId or --all>" + NEWLINE);
		toReturn.append(COMMAND.PREMERGE.toString() + " [<metadataId> [<version>]] [--usedatabases]" + NEWLINE);
		toReturn.append(COMMAND.MERGE.toString() + " [<metadataId> [<version>]] [--force]"+ NEWLINE);
		toReturn.append(COMMAND.EXPORT.toString() 
			+ " <dbName or pathway_id> <uri,uri,.. or --all> <outfile>" +
			" (dbName - any supported by PaxtoolsDAO DB; " +
			"pathway_id is a PK of the pathwayData table - to extract 'premerged' data)" + NEWLINE);
		toReturn.append(COMMAND.EXPORT_VALIDATION.toString() 
			+ " <pathway_id> <output_file> (pathway_id - same as above; output_file " +
			"will contain the Validator Response XML unless '.html' file extension is used, " +
			"in wich case the XML is there auto-transformed to offline HTML+Javascript content)" + NEWLINE);
        toReturn.append(COMMAND.CREATE_BLACKLIST.toString() + " <databaseName> <outfile>"  + NEWLINE);

		return toReturn.toString();
	}

    /**
     * The big deal main.
     * 
     * @param args String[]
     */    
    public static void main(String[] args) throws Exception {
        // sanity check
        if (args.length == 0) {
            System.err.println("Missing args to Admin.");
			System.err.println(Admin.usage());
            System.exit(-1);
        }
    	
    	// "CPATH2_HOME" env. var. must be set (mainly for log4j config)
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
    	PropertyConfigurator.configure(home + File.separator 
    			+ "log4j.properties");

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
    	
		Admin admin = new Admin();
		try {
			admin.setCommandParameters(args);
			admin.run();
		} catch (Exception e) {
			System.err.println(e + NEWLINE + usage());
		}
		// required because MySQL Statement Cancellation Timer thread is still running
		System.exit(0);
    }
    
}
