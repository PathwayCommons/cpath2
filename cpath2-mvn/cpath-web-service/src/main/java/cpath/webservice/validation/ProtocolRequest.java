/**
 ** Copyright (c) 2010 Memorial Sloan-Kettering Cancer Center (MSKCC)
 ** and University of Toronto (UofT).
 **
 ** This is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** any later version.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** both UofT and MSKCC have no obligations to provide maintenance, 
 ** support, updates, enhancements or modifications.  In no event shall
 ** UofT or MSKCC be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** UofT or MSKCC have been advised of the possibility of such damage.  
 ** See the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this software; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA;
 ** or find it at http://www.fsf.org/ or http://www.gnu.org.
 **/

package cpath.webservice.validation;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;

import cpath.service.CPathService.OutputFormat;
import cpath.service.internal.ProtocolStatusCode;
import cpath.webservice.args.Cmd;
import cpath.webservice.args.ProtocolVersion;

import java.util.*;

/**
 * Encapsulates web request from client applications.
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
    private Cmd command;

    /**
     * Query Parameter.
     */
    private String query;

    /**
     * Format.
     */
    private OutputFormat output;

    /**
     * Version.
     */
    private ProtocolVersion version;

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
    private Integer organism;

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
	private String binaryInteractionRule; // comma-separated names

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
        this.version = ProtocolVersion.VERSION_3;
        this.startIndex = 0;
        this.organism = null;
        this.maxHits = null;
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
     * Constructor that builds from the Map.
     * 
     * @param map
     * @throws ProtocolException
     */
    public ProtocolRequest(Map<String, String> map) throws ProtocolException {
		setVersion(map.get(ARG_VERSION));
		setCommand(map.get(ARG_COMMAND));
		setQuery(map.get(ARG_QUERY));
		setOutput(map.get(ARG_OUTPUT));
		setBinaryInteractionRule(map.get(ARG_BINARY_INTERACTION_RULE)); // comma-separated rule names
		setDataSource(map.get(ARG_DATA_SOURCE));
		setInputIDType(map.get(ARG_INPUT_ID_TYPE));
		setOrganism(map.get(ARG_ORGANISM));
		setOutputIDType(map.get(ARG_OUTPUT_ID_TYPE));
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
    public Cmd getCommand() {
        return command;
    }

    /**
     * Gets the Format value.
     *
     * @return output value.
     */
    public OutputFormat getOutput() {
        return output;
    }

    /**
     * Gets the Version value.
     *
     * @return version value.
     */
    public ProtocolVersion getVersion() {
        return version;
    }

    /**
     * Sets Command Argument.
     *
     * @param command Command Argument.
     */
    public void setCommand(Cmd command) {
        this.command = command;
    }

    
    public void setCommand(String command) throws ProtocolException {
		if(command != null) {
			try {
				this.command = Cmd.valueOf(command.trim().toUpperCase());
			} catch (IllegalArgumentException e) {
				throw new ProtocolException(ProtocolStatusCode.BAD_COMMAND,
					"Illegal command; must be one of: " 
					+ Cmd.values().toString());
			}
		} 
		// for null, check later
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
     * @param output Format Argument.
     */
    public void setOutput(OutputFormat format) {
        this.output = format;
    }

    
    public void setOutput(String format) throws ProtocolException {
		if (format != null) {
			try {
				this.output = OutputFormat.valueOf(format.trim().toUpperCase());
			} catch (IllegalArgumentException e) {
				throw new ProtocolException(ProtocolStatusCode.BAD_FORMAT,
						ProtocolRequest.ARG_OUTPUT + " must be one of: " 
						+ OutputFormat.values().toString());
			}
		} else {
			this.output = OutputFormat.XML;
		}
    }
    
    /**
     * Sets the Version Argument.
     *
     * @param version Version Argument.
     */
    public void setVersion(ProtocolVersion version) {
        this.version = version;
    }

    
    public void setVersion(String version) throws ProtocolException {
        if(version != null) {
        	this.version = ProtocolVersion
        		.fromValue(version.trim().toUpperCase());
        	if(this.version == null)
        		throw new ProtocolException(ProtocolStatusCode.VERSION_NOT_SUPPORTED,
						"Supported protocol versions are: " 
						+ ProtocolVersion.versionNumbers().toString());
        }
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
    public Integer getOrganism() {
        return this.organism;
    }

    /**
     * Sets Organism.
     *
     * @param organism Organism String.
     */
    public void setOrganism(Integer organism) {
        this.organism = organism;
    }
    
	public void setOrganism(String organism) throws ProtocolException {
		if (organism != null) {
			try {
				setOrganism(Integer.valueOf(organism));
			} catch (NumberFormatException e) {
				throw new ProtocolException(ProtocolStatusCode.INVALID_ARGUMENT,
						ProtocolRequest.ARG_ORGANISM + " must be Taxonomy ID");
			}
		}
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
                rules[i] = rules[i].trim().toUpperCase();
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
                dataSources[i] = dataSources[i].trim().toUpperCase();
            }
            return dataSources;
        } else {
            return null;
        }
    }

    /**
     * Sets Data Source (comma-separated names list).
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
            list.add(new NameValuePair(ARG_VERSION, version.value));
        }
        if (command != null) {
            list.add(new NameValuePair(ARG_COMMAND, command.name().toUpperCase()));
        }
        if (query != null) {
            list.add(new NameValuePair(ARG_QUERY, query));
        }
        if (output != null) {
            list.add(new NameValuePair(ARG_OUTPUT, output.name().toUpperCase()));
        }
        if (startIndex != 0) {
            list.add(new NameValuePair(ARG_START_INDEX,
                    Long.toString(startIndex)));
        }
        if (organism != null) {
            list.add(new NameValuePair(ARG_ORGANISM, organism.toString()));
        }
        if (maxHits != null) {
            list.add(new NameValuePair(ARG_MAX_HITS, maxHits));
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
