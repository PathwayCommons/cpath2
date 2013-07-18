/**
 * 
 */
package cpath.query;

import java.util.Arrays;

import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.model.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import cpath.client.CPathClient;
import cpath.client.CPathClient.Direction;
import cpath.client.util.CPathException;
import cpath.service.Cmd;
import cpath.service.CmdArgs;
import cpath.service.GraphType;
import cpath.service.OutputFormat;

/**
 * A query to be executed with {@link CPathClient}
 * 
 * @author rodche
 */
public final class CPathGraphQuery extends BasicCPathQuery<Model> implements
		CPathQuery<Model> {

	private boolean mergeEquivalentInteractions = false;
    private Integer limit = 1;
    private Direction direction;
    private GraphType graphType = GraphType.NEIGHBORHOOD;
	private String[] source;
	private String[] target;

	/**
	 * @return the request
	 */
	protected MultiValueMap<String, String> getRequestParams() {
		MultiValueMap<String, String> request = new LinkedMultiValueMap<String, String>();

		// common options for all graph commands
		request.add(CmdArgs.kind.name(), graphType.name());
		request.add(CmdArgs.limit.name(), limit.toString());
		
		if(source == null || source.length == 0)
			throw new IllegalArgumentException("Required 'source' " +
					"parameter (cannot be null or empty)");
		request.put(CmdArgs.source.name(), Arrays.asList(source));
		
		if(organism != null)
			request.put(CmdArgs.organism.name(), Arrays.asList(organism));
		if(datasource != null)
			request.put(CmdArgs.datasource.name(), Arrays.asList(datasource));

		switch (graphType) {
		case COMMONSTREAM:
			if (direction != null) {
				if (direction == Direction.BOTHSTREAM)
					throw new IllegalArgumentException(
							"Direction of common-stream query should be either upstream or downstream.");
				else
					request.add(CmdArgs.direction.name(), direction.name());
			}
			break;
		case PATHSBETWEEN:
			break;
		case PATHSFROMTO:
			request.put(CmdArgs.target.name(), Arrays.asList(target));
			break;
		case NEIGHBORHOOD:
		default:
			if (direction != null)
				request.add(CmdArgs.direction.name(), direction.name());
			break;
		}

		return request;
	}

	/**
	 * Constructor.
	 * @param client cpath2 client instance 
	 */
	public CPathGraphQuery(CPathClient client) {
		this.limit = 1;
		this.client = client;
		this.graphType = GraphType.NEIGHBORHOOD;
	}

	/**
	 * Biopax graph query type (kind)
	 * @param graphType
	 * @return
	 */
	public CPathGraphQuery kind(GraphType graphType) {
		this.graphType = graphType;
		return this;
	}

	/**
	 * The list of URIs or IDs (e.g., gene symbols) 
	 * of <em>source</em> biopax elements (required  
	 * for all graph query types).
	 * @param sources
	 * @return
	 */
	public CPathGraphQuery sources(String[] sources) {
		this.source = sources;
		return this;
	}

	/**
	 * The list of URIs or IDs (e.g., gene symbols) 
	 * of <em>target</em> elements used by PathsFromTo 
	 * queries.
	 * @param targets
	 * @return
	 */
	public CPathGraphQuery targets(String[] targets) {
		this.target = targets;
		return this;
	}

	/**
     * Graph query search distance limit (default = 1);
     * required by all but PathsFromTo queries.
	 * @param limit
	 * @return
	 */
	public CPathGraphQuery limit(Integer limit) {
		this.limit = limit;
		return this;
	}

	/**
	 * Graph query direction. 
	 * The default depends on the graph query type.
	 * It's usually BOTHSTREAM, except for CommonStream query type, 
	 * or DOWNSTREAM (Note: BOTHSTREAM direction is incompatible 
	 * with CommonStream graph queries)
	 * 
	 * @param direction
	 * @return
	 */
	public CPathGraphQuery direction(Direction direction) {
		this.direction = direction;
		return this;
	}

	/**
	 * Sets the option to merge equivalent interactions in the result model.
	 * 
	 * @param mergeEquivalentInteractions
	 */
	public void mergeEquivalentInteractions(boolean mergeEquivalentInteractions) {
		this.mergeEquivalentInteractions = mergeEquivalentInteractions;
	}

	@Override
	public String stringResult(OutputFormat format) throws CPathException {
		MultiValueMap<String, String> request = getRequestParams();
		if (format == null)
			format = OutputFormat.BIOPAX;
		request.add(CmdArgs.format.name(), format.name());

		return client.post(Cmd.GRAPH.toString(), request, String.class);
	}

	@Override
	public Model result() throws CPathException {
		Model model = client.post(Cmd.GRAPH.toString(), getRequestParams(),
				Model.class);

		if (mergeEquivalentInteractions && model != null) {
			ModelUtils.mergeEquivalentInteractions(model);
		}

		return model;
	}

}
