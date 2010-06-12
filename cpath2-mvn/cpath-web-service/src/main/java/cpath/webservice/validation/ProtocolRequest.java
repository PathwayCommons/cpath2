// $Id: ProtocolRequest.java,v 1.36 2009/10/05 18:03:49 cerami Exp $
//------------------------------------------------------------------------------
/** Copyright (c) 2006 Memorial Sloan-Kettering Cancer Center.
 **
 ** Code written by: Ethan Cerami
 ** Authors: Ethan Cerami, Gary Bader, Chris Sander
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

package cpath.webservice.validation;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;

import cpath.webservice.args.ProtocolVersion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates Request object from client/browser application.
 *
 * @author Ethan Cerami
 * 
 * @author rodche - fitting into cPathSquared
 */
public class ProtocolRequest {

    /**
     * Command Argument.
     */
    public static final String ARG_COMMAND = "cmd";

    /**
     * Uid Argument.
     */
    public static final String ARG_QUERY = "q";

    /**
     * Format Argument.
     */
    public static final String ARG_FORMAT = "output"; // was "format"

    /**
     * Version Argument.
     */
    public static final String ARG_VERSION = "version";

    /**
     * Start Index Argument.
     */
    public static final String ARG_START_INDEX = "startIndex";

    /**
     * Max Hits Argument.
     */
    public static final String ARG_MAX_HITS = "maxHits";

    /**
     * Organism Argument.
     */
    public static final String ARG_ORGANISM = "organism";

    /**
     * Check cache (undocumented argument, used for debugging purposes only)
     * Default is set to true.
     */
    public static final String ARG_CHECK_XML_CACHE = "checkXmlCache";

    /**
     * Use optimized code (undocument argument, used for debugging purposes only)
     * Default is set to true.
     */
    public static final String ARG_USE_OPTIMIZED_CODE = "useOptimizedCode";

	/**
	 * Internal ID Type - used in some newer web service api calls,
	 * like get_neighbors & get_pathway_list.
	 */
	public static final String INTERNAL_ID = "internal_id";

	/**
	 * ID List Type - used in some newer web service api calls,
	 * like get_neighbors.
	 */
	public static final String ID_LIST = "id_list";

	/**
	 * Input ID Type.
	 *
	 * An optional parameter to get_neighbors command which
	 * specifies the database identifier used for input. If
	 * this is not set, cPath internal id is assumed.
	 */
	public static final String ARG_INPUT_ID_TYPE = "input_id_type";

	/**
	 * Neighborhood Title.
	 *
	 * An optional/undocumented parameter to get_neighbors.  When set,
	 * this string will be used as a title to the respective CyNetworkView
	 * used in Cytoscape.
	 */
	public static final String ARG_NEIGHBORHOOD_TITLE = "neighborhood_title";

	/**
	 * Fully Connected.
	 *
	 * An optional parameter to get_neighbors.  When set to no (default),
	 * the physical entity and connections to its nearest neighbors
	 * are returned; when set to yes, all connections between all
	 * physical entities are returned.
	 */
	public static final String ARG_FULLY_CONNECTED = "fully_connected";

	/**
	 * Binary Interaction Rule(s).
	 *
	 * An optional parameter to get_neighbors & get_record_by_cpath_id. This is
	 * only relevant when user requests binary interactions (ie BINARY_SIF),
	 * as output type.  This optional parameter will specify which rules need
	 * to be governed when binary interactions are created.  If binary interactions
	 * are requested, and binary interaction rules are not specified, all rules 
	 * will be used.
	 */
	public static final String ARG_BINARY_INTERACTION_RULE = "binary_interaction_rule";

	/**
	 * Output.
	 *
	 * An optional parameter to specify the output format.
     * When set to biopax (default), the output is a biopax representation of the nearest
     * neighbor "pathway".  When the output is set to id_list, a set of id's in plain text is
     * returned.
	 */
	public static final String ARG_OUTPUT = "output";

	/**
	 * Output ID Type.
	 *
	 * An optional parameter which specifies the database identifiers used in the output.
	 * This argument is only relevant when output=id_list.
	 */
	public static final String ARG_OUTPUT_ID_TYPE = "output_id_type";
	
	/**
	 * Data Source.
	 *
	 * An optional parameter which filters the output by data source.
	 */
	public static final String ARG_DATA_SOURCE = "data_source";


    /**
     * Command.
     */
    private String command;

    /**
     * Query Parameter.
     */
    private String query;

    /**
     * Format.
     */
    private String format;

    /**
     * Version.
     */
    private String version;

    /**
     * Start Index.
     */
    private int startIndex;

    /**
     * Max Hits.
     */
    private String maxHits;

    /**
     * Organism Parameter.
     */
    private String organism;

    /**
     * Check XML cache parameter.
     */
    private boolean checkXmlCache;

    /**
     * Use optimized code parameter.
     */
    private boolean useOptimizedCode;

	/**
	 * Input ID Type Parameter.
	 * (see ProtocolRequest.ARG_INPUT_ID_TYPE)
	 */
	private String inputIDType;

	/**
	 * Fully Connected Parameter.
	 * (see ProtocolRequest.ARG_FULLY_CONNECTED)
	 */
	private String fullyConnected;

	/**
	 * Binary Interaction Rule Parameter.
	 * (see ProtocolRequest.ARG_BINARY_INTERACTION_RULE)
	 */
	private String binaryInteractionRule;

	/**
	 * Output Parameter.
	 * (see ProtocolRequest.ARG_OUTPUT)
	 */
	private String output;

	/**
	 * Output ID Type Parameter.
	 * (see ProtocolRequest.ARG_OUTPUT_ID_TYPE)
	 */
	private String outputIDType;

	/**
	 * Data Source Parameter.
	 * (see ProtocolRequest.ARG_DATA_SOURCE)
	 */
	private String dataSource;


    /**
     * EmptyParameterSet.
     */
    private boolean emptyParameterSet;

    private static final char DOUBLE_QUOTE = '"';
    private static final char SINGLE_QUOTE = '\'';

    /**
     * Constructor.
     */
    public ProtocolRequest() {
        this.version = ProtocolVersion.VERSION_2.name();
        this.startIndex = 0;
        this.organism = null;
        this.maxHits = null;
        this.checkXmlCache = true;
        this.useOptimizedCode = true;
		// start get_neighbors parameters
		this.inputIDType = null;
		this.fullyConnected = null;
		this.binaryInteractionRule = null;
		this.output = null;
		this.outputIDType = null;
		this.dataSource = null;
		// end get_neighbors parameters
    }


    /**
     * Constructor.
     *
     * @param parameterMap Map of all Request Parameters.
     */
    public ProtocolRequest(Map<String,String> parameterMap) {
        this.command = parameterMap.get(ProtocolRequest.ARG_COMMAND);
        this.query = parameterMap.get(ProtocolRequest.ARG_QUERY);
        this.query = massageQuery(query);
        this.format = parameterMap.get(ProtocolRequest.ARG_FORMAT);
        this.version = parameterMap.get(ProtocolRequest.ARG_VERSION);
        this.maxHits = parameterMap.get(ProtocolRequest.ARG_MAX_HITS);
        this.organism = parameterMap.get
                (ProtocolRequest.ARG_ORGANISM);
        if (maxHits == null) {
            maxHits = "25";
        }
        String startStr =
                (String) parameterMap.get(ProtocolRequest.ARG_START_INDEX);
        if (startStr != null) {
            this.startIndex = Integer.parseInt(startStr);
        } else {
            this.startIndex = 0;
        }
        String checkXmlCacheStr = (String) parameterMap.get
                (ProtocolRequest.ARG_CHECK_XML_CACHE);
        if (checkXmlCacheStr != null && checkXmlCacheStr.equals("0")) {
            checkXmlCache = false;
        } else {
            checkXmlCache = true;
        }

        String useOptimizedCodeStr = (String) parameterMap.get
                (ProtocolRequest.ARG_USE_OPTIMIZED_CODE);
        if (useOptimizedCodeStr != null && useOptimizedCodeStr.equals("0")) {
            useOptimizedCode = false;
        } else {
            useOptimizedCode = true;
        }

        // start get_neighbors parameters
		this.inputIDType = (String)parameterMap.get(ProtocolRequest.ARG_INPUT_ID_TYPE);
		this.fullyConnected = (String)parameterMap.get(ProtocolRequest.ARG_FULLY_CONNECTED);
		this.binaryInteractionRule = (String)parameterMap.get(ProtocolRequest.ARG_BINARY_INTERACTION_RULE);
		this.output = (String)parameterMap.get(ProtocolRequest.ARG_OUTPUT);
		this.outputIDType = (String)parameterMap.get(ProtocolRequest.ARG_OUTPUT_ID_TYPE);
		this.dataSource = (String)parameterMap.get(ProtocolRequest.ARG_DATA_SOURCE);
		// end get_neighbors parameters

        if (parameterMap.size() == 0) {
            emptyParameterSet = true;
        } else {
            emptyParameterSet = false;
        }
    }

    /**
     * Massages the UID such that No Database Error Occur.
     * 0.  Trim and make upper case.
     * 1.  Replace single quotes with double quotes.
     */
    private String massageQuery(String temp) {
        if (temp != null && temp.length() > 0) {
            temp = temp.replace(SINGLE_QUOTE, DOUBLE_QUOTE);
            String parts[] = query.trim().split("\\s+");
            StringBuffer buf = new StringBuffer ();
            for (String part:  parts) {
                buf.append (part + " ");
            }
            return buf.toString().trim();
        } else {
            return null;
        }
    }

    /**
     * Gets the Query value.
     *
     * @return query value.
     */
    public String getQuery() {
        return query;
    }

    /**
     * Gets the Command value.
     *
     * @return command value.
     */
    public String getCommand() {
        return command;
    }

    /**
     * Gets the Format value.
     *
     * @return format value.
     */
    public String getFormat() {
        return format;
    }

    /**
     * Gets the Version value.
     *
     * @return version value.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets Command Argument.
     *
     * @param command Command Argument.
     */
    public void setCommand(String command) {
        this.command = command;
    }

    /**
     * Sets the Query Argument.
     *
     * @param query Query Argument.
     */
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * Sets the Format Argument.
     *
     * @param format Format Argument.
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * Sets the Version Argument.
     *
     * @param version Version Argument.
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Gets the Start Index.
     *
     * @return Start Index.
     */
    public int getStartIndex() {
        return startIndex;
    }

    /**
     * Sets the Start Index.
     *
     * @param startIndex Start Index Int.
     */
    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    /**
     * Gets Max Number of Hits.
     *
     * @return Max Number Hits
     */
    public int getMaxHitsInt() {
        if (maxHits.equals("unbounded")) {
            return Integer.MAX_VALUE;
        } else {
            return Integer.parseInt(maxHits);
        }
    }

    /**
     * Max hits (String value).
     *
     * @return Max Hits (String value.)
     */
    public String getMaxHits() {
        return this.maxHits;
    }

    /**
     * Sets Max Number of hits.
     *
     * @param str Max Number of Hits
     */
    public void setMaxHits(String str) {
        this.maxHits = str;
    }

    /**
     * Gets Organism.
     *
     * @return Organism String.
     */
    public String getOrganism() {
        return this.organism;
    }

    /**
     * Sets Organism.
     *
     * @param organism Organism String.
     */
    public void setOrganism(String organism) {
        this.organism = organism;
    }

    /**
     * Gets the Check XML Cache Flag.
     * @return true or false.
     */
    public boolean getCheckXmlCache () {
        return this.checkXmlCache;
    }

    /**
     * Sets the Check XML Cache Flag.
     * @param flag true or false.
     */
    public void setCheckXmlCache(boolean flag){
        this.checkXmlCache = flag;
    }

    /**
     * Gets the use optimized code flag.
     * @return flag true or false.
     */
    public boolean getUseOptimizedCode() {
        return this.useOptimizedCode;
    }

    /**
     * Sets the use optimized code flag.
     * @param flag true or false.
     */
    public void setUseOptimizedCode (boolean flag) {
        this.useOptimizedCode = flag;
    }

    /**
     * Gets Input ID Type.
     *
     * @return String
     */
    public String getInputIDType() {
        return this.inputIDType;
    }

    /**
     * Sets Input ID Type.
     *
     * @param inputIDType String
     */
    public void setInputIDType(String inputIDType) {
        this.inputIDType = inputIDType;
    }

    /**
     * Gets fully connected.
	 *
     * @return String
     */
    public String getFullyConnected() {
        return this.fullyConnected;
    }

    /**
     * Sets fully connected.
	 *
     * @param fullyConnected fully connected string.
     */
    public void setFullyConnected(String fullyConnected) {
        this.fullyConnected = fullyConnected;
    }

    /**
     * Gets binary interaction rule.
	 *
     * @return String
     */
    public String getBinaryInteractionRule() {
        return this.binaryInteractionRule;
    }

    /**
     * Gets list of binary interaction rules.
	 *
     * @return String[]
     */
    public String[] getBinaryInteractionRules() {
		if (binaryInteractionRule != null &&
			binaryInteractionRule.trim().length() > 0) {
            //  Split by comma, and then trim
            String rules[] = binaryInteractionRule.split(",");
            for (int i=0; i< rules.length; i++) {
                rules[i] = rules[i].trim();
            }
            return rules;
        } else {
            return null;
        }
    }

    /**
     * Sets binary interaction rule.
	 *
     * @param binaryInteractionRule binary interaction rule string.
     */
    public void setBinaryInteractionRule(String binaryInteractionRule) {
        this.binaryInteractionRule = binaryInteractionRule;
    }

    /**
     * Gets Output.
     *
     * @return String
     */
    public String getOutput() {
        return this.output;
    }

    /**
     * Sets Output.
     *
     * @param output String
     */
    public void setOutput(String output) {
        this.output = output;
    }

    /**
     * Gets Output ID Type.
     *
     * @return String
     */
    public String getOutputIDType() {
        return this.outputIDType;
    }

    /**
     * Sets Output ID Type.
     *
     * @param outputIDType String
     */
    public void setOutputIDType(String outputIDType) {
        this.outputIDType = outputIDType;
    }

    /**
     * Gets Data Source.
     *
     * @return String
     */
    public String getDataSource() {
        return this.dataSource;
    }

    /**
     * Get list of all data sources.
     * @return array of all data sources.
     */
    public String[] getDataSources() {
        if (dataSource != null && dataSource.trim().length() > 0) {
            //  Split by comma, and then trim
            String dataSources[] = dataSource.split(",");
            for (int i=0; i<dataSources.length; i++) {
                dataSources[i] = dataSources[i].trim();
            }
            return dataSources;
        } else {
            return null;
        }
    }

    /**
     * Sets Data Source.
     *
     * @param dataSource String
     */
    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }


    /**
     * Is this an empty request?
     *
     * @return true or false.
     */
    public boolean isEmpty() {
        return this.emptyParameterSet;
    }

    /**
     * Gets URI.
     *
     * @return URI String.
     */
    public String getUri() {
        GetMethod method = new GetMethod("webservice.do");
        return createUri(method);
    }

    /**
     * Gets URL Parameter String
     *
     * @return URL Parameter String.
     */
    public String getUrlParameterString() {
        GetMethod method = new GetMethod();
        return createUri(method).substring(1);
    }

    private String createUri(GetMethod method) {
        String uri;
        List<NameValuePair> list = new ArrayList<NameValuePair>();
        if (version != null) {
            list.add(new NameValuePair(ARG_VERSION, version));
        }
        if (command != null) {
            list.add(new NameValuePair(ARG_COMMAND, command));
        }
        if (query != null) {
            list.add(new NameValuePair(ARG_QUERY, query));
        }
        if (format != null) {
            list.add(new NameValuePair(ARG_FORMAT, format));
        }
        if (startIndex != 0) {
            list.add(new NameValuePair(ARG_START_INDEX,
                    Long.toString(startIndex)));
        }
        if (organism != null) {
            list.add(new NameValuePair(ARG_ORGANISM, organism));
        }
        if (maxHits != null) {
            list.add(new NameValuePair(ARG_MAX_HITS, maxHits));
        }
        
        if (checkXmlCache == false) {
            list.add(new NameValuePair(ARG_CHECK_XML_CACHE, "0"));
        }
        if (useOptimizedCode == false) {
            list.add(new NameValuePair(ARG_USE_OPTIMIZED_CODE, "0"));
        }

		if (inputIDType != null) {
            list.add(new NameValuePair(ARG_INPUT_ID_TYPE, inputIDType));
		}
		if (fullyConnected != null) {
			list.add(new NameValuePair(ARG_FULLY_CONNECTED, fullyConnected));
		}
		if (binaryInteractionRule != null) {
			list.add(new NameValuePair(ARG_BINARY_INTERACTION_RULE, binaryInteractionRule));
		}
		if (output != null) {
            list.add(new NameValuePair(ARG_OUTPUT, output));
		}
		if (outputIDType != null) {
            list.add(new NameValuePair(ARG_OUTPUT_ID_TYPE, outputIDType));
		}
		if (dataSource != null) {
            list.add(new NameValuePair(ARG_DATA_SOURCE, dataSource));
		}

        NameValuePair nvps[] = (NameValuePair[])
                list.toArray(new NameValuePair[list.size()]);
        method.setQueryString(nvps);
        try {
            uri = method.getURI().getEscapedURI();
            uri = uri.replaceAll("&", "&amp;");
        } catch (URIException e) {
            uri = null;
        }
        return uri;
    }
}
