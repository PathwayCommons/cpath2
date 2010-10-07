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

import cpath.dao.PaxtoolsDAO;
import cpath.dao.internal.DataServicesFactoryBean;
import cpath.fetcher.CPathFetcher;
import cpath.fetcher.WarehouseDataService;
import cpath.fetcher.internal.CPathFetcherImpl;
import cpath.importer.Merger;
import cpath.importer.internal.MergerImpl;
import cpath.importer.internal.PremergeDispatcher;
import cpath.warehouse.MetadataDAO;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.PathwayData;

import org.biopax.paxtools.model.Model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Collection;

import static cpath.config.CPathSettings.*;

/**
 * Class which provides command line admin capabilities.
 */
public class Admin implements Runnable {
	private static final Log LOG = LogFactory.getLog(Admin.class);
	// used as a argument to fetch-pathwaydata
	private static final String FETCH_ALL = "all";
	
    // COMMAND Enum
    public static enum COMMAND {

        // command types
    	CREATE_TABLES("-create-tables"),
        FETCH_METADATA("-fetch-metadata"),
		FETCH_DATA("-fetch-data"),
		PREMERGE("-premerge"),
		MERGE("-merge");

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
			}
			else {
				this.command = COMMAND.FETCH_DATA;
				this.commandParameters = new String[] { args[1] };
			}
		}
		else if (args[0].equals(COMMAND.PREMERGE.toString())) {
			this.command = COMMAND.PREMERGE;
			// takes no args
			this.commandParameters = new String[] { "" };
		}
		else if (args[0].equals(COMMAND.MERGE.toString())) {
			this.command = COMMAND.MERGE;
			// takes no args
			this.commandParameters = new String[] { "" };
		}
        else {
            validArgs = false;
        }

        if (!validArgs) {
			System.err.println(usage());
            throw new IllegalArgumentException("Invalid command passed to Admin.");
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
            			// create db schema and lucene index
            			DataServicesFactoryBean.createSchema(dbName);
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
				ApplicationContext context =
                    new ClassPathXmlApplicationContext(new String [] { 	
                    		"classpath:applicationContext-whouseDAO.xml", 
                    		"classpath:applicationContext-biopaxValidation.xml", 
        					"classpath:applicationContext-cpathPremerger.xml",
        					"classpath:applicationContext-cvRepository.xml"});
                PremergeDispatcher premergeDispatcher = (PremergeDispatcher) context.getBean("premergeDispatcher");
				premergeDispatcher.start();
				// sleep until premerge is complete, this is required so we can call System.exit(...) below
				premergeDispatcher.join();
				break;
			case MERGE:
				// pc dao
				context = new ClassPathXmlApplicationContext("classpath:applicationContext-cpathDAO.xml");
				final PaxtoolsDAO pcDAO = (PaxtoolsDAO)context.getBean("paxtoolsDAO");
				// merger
				Merger merger = new MergerImpl((Model)pcDAO);
				merger.merge();
				break;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
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
        CPathFetcher providerMetadataService = new CPathFetcherImpl();
    	
        // grab the data
        Collection<Metadata> metadata = providerMetadataService.getMetadata(location);
        
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
        WarehouseDataService fetcher = new CPathFetcherImpl();
    	
		// get metadata
		Collection<Metadata> metadataCollection = getMetadata(metadataDAO, provider);

		// sanity check
		if (metadataCollection == null || metadataCollection.size() == 0) {
			LOG.error("Unknown provider: " + provider);
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
			// fetch (download or copy to a sub-directory in CPATH2_HOME)
			try {
				((CPathFetcher)fetcher).fetchData(metadata);
			} catch (IOException e) {
				LOG.error("Failed fetching data for " + metadata.toString() 
					+ ". Skipping...", e);
				continue;
			}
			
			
			if (metadata.getType() == Metadata.TYPE.PSI_MI ||
					metadata.getType() == Metadata.TYPE.BIOPAX ||
					metadata.getType() == Metadata.TYPE.BIOPAX_L2) 
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
							((CPathFetcher)fetcher)
								.getProviderPathwayData(metadata);
	        
						// process pathway data
						for (PathwayData pwData : pathwayData) {
							metadataDAO.importPathwayData(pwData);
						}
					}
			} 
			else if (metadata.getType() == Metadata.TYPE.PROTEIN) 
			{
				// parse/save
				fetcher.storeWarehouseData(metadata, proteinsDAO);
        	} 
			else if (metadata.getType() == Metadata.TYPE.SMALL_MOLECULE) 
			{
		        // parse/save
				fetcher.storeWarehouseData(metadata, smallMoleculesDAO);
			} 
			else if (metadata.getType() == Metadata.TYPE.MAPPING) {
				// Nothing else to do
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
		return (provider.equalsIgnoreCase(FETCH_ALL))
			? metadataDAO.getAll()
			: Collections.singleton(metadataDAO.getMetadataByIdentifier(provider));
	}

	
	private static String usage() {

		StringBuffer toReturn = new StringBuffer();
		toReturn.append("cpath.Admin <command> <one or more args>" + NEWLINE);
		toReturn.append("commands:" + NEWLINE);
		toReturn.append(COMMAND.CREATE_TABLES.toString() + " <table1,table2,..>" + NEWLINE);
		toReturn.append(COMMAND.FETCH_METADATA.toString() + " <url>" + NEWLINE);
		toReturn.append(COMMAND.FETCH_DATA.toString() + " <provider-name or all>" + NEWLINE);
		toReturn.append(COMMAND.PREMERGE.toString() + NEWLINE);
		toReturn.append(COMMAND.MERGE.toString() + NEWLINE);

		// outta here
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
    	
    	String home = System.getenv(HOME_VARIABLE_NAME);
    	
    	if (home==null) {
            System.err.println("Please set " + HOME_VARIABLE_NAME 
            	+ " environment variable " +
            	" (point to a directory where cpath.properties, etc. files are placed)");
            System.exit(-1);
    	}
    	
    	// configure logging
    	PropertyConfigurator.configure(home + File.separator 
    			+ "log4j.properties");
    	
    	// set JVM property to be used by other modules (in spring context)
    	System.setProperty(HOME_VARIABLE_NAME, home);


		Admin admin = new Admin();
		admin.setCommandParameters(args);
        admin.run();
		// required because MySQL Statement Cancellation Timer thread is still running
		System.exit(0);
    }
    
}
