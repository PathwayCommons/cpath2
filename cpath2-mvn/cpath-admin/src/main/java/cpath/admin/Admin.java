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
import cpath.fetcher.metadata.Metadata;

import org.apache.log4j.PropertyConfigurator;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;
import java.util.Collection;

/**
 * An interface which provides methods to query a Paxtools model.
 */
public class Admin implements Runnable {

    // COMMAND Enum
    public static enum COMMAND {

        // command types
        FETCH_METADATA("-fetch-metadata");

        // string ref for readable name
        private String command;
        
        // contructor
        COMMAND(String command) { this.command = command; }

        // method to get enum readable name
        public String toString() { return command; }
    }

    // ref to fetcher
    private CPathFetcher cpathFetcher;

    // command, command parameter
    private COMMAND command;
    private String commandParameter;

    /**
     * Constructor.
     *
     * @param args String[]
     * @param cpathFetcher CPathFetcher
     */
    public Admin(final String[] args, final CPathFetcher cpathFetcher) {
        
        // init members
        this.cpathFetcher = cpathFetcher;

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
            this.command = COMMAND.FETCH_METADATA;
            this.commandParameter = args[1];
        }
        else {
            validArgs = false;
        }

        if (!validArgs) {
            throw new IllegalArgumentException("Invalid command passed to Admin.");
        }
    }

    @Override
    public void run() {

        try {
            switch (command) {
            case FETCH_METADATA:
                fetchMetadata(commandParameter);                
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
        
        // TODO: process metadata
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
            System.exit(-1);
        }

        // configure log4j
        PropertyConfigurator.configure(Admin.class.getClassLoader().getResource("log4j.properties"));

        // setup context
        ApplicationContext context =
            new ClassPathXmlApplicationContext(new String [] { "classpath:applicationContext-cpathFetcher.xml",
                                                               "classpath:applicationContext-cpathAdmin.xml" });

        // TODO: inject
        CPathFetcher cpathFetcher = (CPathFetcher)context.getBean("cpathFetcher");
        Admin admin = new Admin(args, cpathFetcher);
        admin.run();
    }
}
