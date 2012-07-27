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
package cpath.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;
import org.biopax.validator.result.ValidatorResponse;
import org.biopax.validator.utils.BiopaxValidatorUtils;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;

import cpath.service.jaxb.DataResponse;
import cpath.service.jaxb.ErrorResponse;
import cpath.service.jaxb.ServiceResponse;

import java.io.*;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import static cpath.config.CPathSettings.*;

/**
 * Console commands to query the cpath2 data
 * without using the web services.
 */
public class Main  {
	private static final Log LOG = LogFactory.getLog(Main.class);
	
    // COMMAND Enum
    public static enum COMMAND 
    {
    	// validation report for a data provider (incl. about all data files)
    	VALIDATION_REPORT("-validation-report"),
    	// export a "valid sub-model" from the main db
    	GET("-get"), //TODO add '--output-format' parameter
        CONVERT("-convert"),
    	;

        private String command;
        
        COMMAND(String command) { this.command = command; }

        public String toString() { return command; }
    }

    // command, command parameter
    private COMMAND command;
    private String[] commandParameters;
    
	
    /**
     * Creates a CPathService instance using
     * the "main" cpath2 DAO.
     * 
     * @return
     */
	private static CPathService getService() {
		ApplicationContext context =
            new ClassPathXmlApplicationContext(new String [] { 	
            		"classpath:applicationContext-cpathDAO.xml",
        			"classpath:applicationContext-whouseDAO.xml",
        			"classpath:applicationContext-cpathService.xml"});
		return (CPathService) context.getBean("service");
	}
	
	
    public void setCommandParameters(String[] args) {
		this.commandParameters = args;
        // parse args
        parseArgs(args);
	}
    
    
    private void parseArgs(final String[] args) {
        boolean validArgs = true;
 
        if(args[0].equals(COMMAND.VALIDATION_REPORT.toString())) {
			if (args.length != 3) {
				validArgs = false;
			} else {
				this.command = COMMAND.VALIDATION_REPORT;
				this.commandParameters = new String[] {args[1], args[2]};
			} 
        } 
        else if(args[0].equals(COMMAND.GET.toString())) {
			if (args.length != 3) {
				validArgs = false;
			} else {
				this.command = COMMAND.GET;
				this.commandParameters = new String[] {args[1], args[2]};
			} 
        } 
        else if(args[0].equals(COMMAND.CONVERT.toString())) {
			if (args.length != 4) {
				validArgs = false;
			}
            else {
				this.command = COMMAND.CONVERT;
				this.commandParameters = new String[] {args[1], args[2], args[3]};
			} 
        } 
        else {
            validArgs = false;
        }
        
        if(!validArgs) {
        	System.err.println("Missing args!" + args.toString());
        	System.exit(-1);
        }
    }


    private void run() {

        try {
            switch (command) {
            case VALIDATION_REPORT:
            	String outf = commandParameters[1];
            	FileOutputStream fos = new FileOutputStream(outf);
            	if(outf.endsWith(".html"))
            		getValidationReport(commandParameters[0], fos, true);
            	else
                	getValidationReport(commandParameters[0], fos, false);
            	
				break;
            case GET:
            	fos = new FileOutputStream(commandParameters[1]);
                fetchAsBiopax(fos, commandParameters[0]);
                
				break;
            case CONVERT:
                String biopax = readFileAsString(commandParameters[0]);
            	fos = new FileOutputStream(commandParameters[1]);
                convert(fos, biopax, commandParameters[2]);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

	/**
	 * 
	 * 
	 * @param output
	 * @param csvIdsString
	 * @throws IOException 
	 */
	public static void convert(OutputStream output, String biopax, String outputFormat) 
			throws IOException 
	{
		ServiceResponse res = getService().convert(biopax, OutputFormat.valueOf(outputFormat));
		if (!(res instanceof ErrorResponse)) {
    		System.err.println(res.toString());
    	} else if (!res.isEmpty()) {
    		String owl = (String) ((DataResponse)res).getData();
    		output.write(owl.getBytes("UTF-8"));
    		output.flush();
    	}
	}

	/**
	 * 
	 * 
	 * @param output
	 * @param csvIdsString
	 * @throws IOException 
	 */
	public static void fetchAsBiopax(OutputStream output, String csvIdsString) 
		throws IOException 
	{
		String[] uris = csvIdsString.split(",");
		ServiceResponse res = getService().fetch(OutputFormat.BIOPAX, uris);
		if(res instanceof ErrorResponse) {
    		System.err.println(res.toString());
    	} else if(!res.isEmpty()) {
    		String owl = (String) ((DataResponse)res).getData();
    		output.write(owl.getBytes("UTF-8"));
    		output.flush();
    	}
	}


	/**
	 * Exports the consolidated validation report (XML or HTML) 
	 * that includes multiple validation results for the 
	 * provider's pathway data from different files/versions.
	 * 
	 * @param provider
	 * @param output
	 * @param asHtml generate HTML report (transformed from the XML)
	 * @throws IOException
	 */
	public static void getValidationReport(final String provider, 
    		final OutputStream output, boolean asHtml) throws IOException 
    {
    	if(LOG.isInfoEnabled()) {
    		LOG.info("Getting validation report for " + provider
    				+ "...");
    	}
    	ServiceResponse res = getService().getValidationReport(provider);
    	if(res instanceof ErrorResponse) {
    		System.err.println(res.toString());
    	} else if(!res.isEmpty()) {
    		ValidatorResponse report = (ValidatorResponse) ((DataResponse)res).getData();
    		OutputStreamWriter writer = new OutputStreamWriter(output, "UTF-8");
    		Source xsl = (asHtml) 
    			? new StreamSource((new DefaultResourceLoader())
    					.getResource("classpath:html-result.xsl").getInputStream())
    			: null;

			BiopaxValidatorUtils.write(report, writer, xsl); 
    		writer.flush();
    	}
	}
	
	
	private static void usage() {
		StringBuilder toReturn = new StringBuilder();
		toReturn.append(Main.class.getCanonicalName()).append(" <command> <one or more args>" + NEWLINE);
		toReturn.append("commands:" + NEWLINE);
		toReturn.append(COMMAND.VALIDATION_REPORT.toString() + " <provider> <output(.xml|.html)>" + NEWLINE);
		toReturn.append(COMMAND.GET.toString() + " <uri1,uri2,..> <output.owl>" + NEWLINE);
		toReturn.append(COMMAND.CONVERT.toString() + " <biopax.owl> <output-file> <output format>" + NEWLINE);
		System.err.println(toReturn.toString());
		System.exit(-1);
	}

    /**
     * Converts incoming biopax file to string.
     */
    private static String readFileAsString(String filePath) throws java.io.IOException {

        StringBuilder fileData = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        char[] buf = new char[1024];
        int numRead=0;
        while ((numRead=reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }
        reader.close();
        return fileData.toString();
    }
	
    /**
     * The big deal main.
     * 
     * @param args String[]
     */    
    public static void main(String[] args) throws Exception {
        // sanity check
        if (args.length == 0) {
			Main.usage(); //exits
        }
    	
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
    	
		Main app = new Main();
		app.setCommandParameters(args);
        app.run();
        
		System.exit(0);
    }
    
}
