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

// imports
import cpath.fetcher.CPathFetcher;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.PathwayData;
import cpath.warehouse.metadata.MetadataDAO;
import cpath.warehouse.pathway.PathwayDataDAO;

import org.apache.log4j.PropertyConfigurator;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;
import java.util.Collection;

/**
 * Class which provides command line admin capabilities.
 */
public class Admin implements Runnable {

    // COMMAND Enum
    public static enum COMMAND {

        // command types
        FETCH_METADATA("-fetch-metadata"),
		FETCH_PATHWAY_DATA("-fetch-pathwaydata");

        // string ref for readable name
        private String command;
        
        // contructor
        COMMAND(String command) { this.command = command; }

        // method to get enum readable name
        public String toString() { return command; }
    }

    // ref to fetcher
    private CPathFetcher cpathFetcher;
    // ref to metadata dao 
	private MetadataDAO metadataDAO;
	// ref to pathway data dao
	private PathwayDataDAO pathwayDataDAO;

    // command, command parameter
    private COMMAND command;
    private String[] commandParameters;

    /**
     * Constructor.
     *
     * @param args String[]
     * @param cpathFetcher CPathFetcher
     * @param metadataDAO MetadataDAO
     */
    public Admin(final String[] args, final CPathFetcher cpathFetcher, final MetadataDAO metadataDAO) {
        
        // init members
        this.cpathFetcher = cpathFetcher;
        this.metadataDAO = metadataDAO;

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
        if (args[0].equals(COMMAND.FETCH_METADATA.toString())) {
			if (args.length != 2) {
				validArgs = false;
			}
			else {
				this.command = COMMAND.FETCH_METADATA;
				this.commandParameters = new String[] { args[1] };
			}
        }
		else if (args[0].equals(COMMAND.FETCH_PATHWAY_DATA.toString())) {
			if (args.length != 2) {
				validArgs = false;
			}
			else {
				this.command = COMMAND.FETCH_PATHWAY_DATA;
				this.commandParameters = new String[] { args[1] };
			}
		}
        else {
            validArgs = false;
        }

        if (!validArgs) {
			System.err.println(usage());
            throw new IllegalArgumentException("Invalid command passed to Admin.");
        }
    }

    @Override
    public void run() {

        try {
            switch (command) {
            case FETCH_METADATA:
                fetchMetadata(commandParameters[0]);                
                break;
			case FETCH_PATHWAY_DATA:
				fetchPathwayData(commandParameters[0]);
				break;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Helper function to get provider metadata.
     *
     * @param url String
     * @throws IOException
     */
    private void fetchMetadata(final String url) throws IOException {

        // grab the data
        Collection<Metadata> metadata = cpathFetcher.getProviderMetadata(url);
        
        // process metadata
        for (Metadata mdata : metadata) {
            metadataDAO.importMetadata(mdata);
        }
    }

    /**
     * Helper function to get provider metadata.
     *
     * @param url String
     * @throws IOException
     */
    private void fetchPathwayData(final String provider) throws IOException {

		// get metadata
		Metadata metadata = metadataDAO.getByIdentifier(provider);

		if (metadata == null) {
			System.err.println("Unknown provider: " + provider);
			return;
		}

		// lets not fetch data if its the same version we have already persisted
		if (metadata.getVersion() > metadata.getPersistedVersion()) {

			// grab the data
			Collection<PathwayData> pathwayData =
				cpathFetcher.getProviderPathwayData(metadata);
        
			// process pathway data
			for (PathwayData pwData : pathwayData) {
				pathwayDataDAO.importPathwayData(pwData);
			}
		}
    }

	public static String usage() {

		StringBuffer toReturn = new StringBuffer();
		toReturn.append("cpath.Admin <command> <one or more args>");
		toReturn.append("commands:");
		toReturn.append(COMMAND.FETCH_METADATA.toString() + " <url>");
		toReturn.append(COMMAND.FETCH_PATHWAY_DATA.toString() + " <provider>");

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

        // configure log4j
        PropertyConfigurator.configure(Admin.class.getClassLoader().getResource("log4j.properties"));

        // setup context
        ApplicationContext context =
            new ClassPathXmlApplicationContext(new String [] { "classpath:applicationContext-cpathFetcher.xml",
                                                               "classpath:applicationContext-cpathImporter.xml",
															   "classpath:applicationContext-cpathWarehouse.xml",
                                                               "classpath:applicationContext-cpathAdmin.xml"});

        // TODO: inject
        CPathFetcher cpathFetcher = (CPathFetcher)context.getBean("cpathFetcher");
        MetadataDAO metadataDAO = (MetadataDAO)context.getBean("metadataDAO");
        Admin admin = new Admin(args, cpathFetcher, metadataDAO);
        admin.run();
    }
}
