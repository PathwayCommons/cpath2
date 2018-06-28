package cpath.service.web.args;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.biopax.paxtools.pattern.miner.SIFType;

import cpath.service.api.OutputFormat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Get extends ServiceQuery {
	@NotNull(message="Illegal Output Format") 
	@Valid
	private OutputFormat format;
	// required at least one value
	@NotEmpty(message="Provide at least one URI.")
	private String[] uri;

	private SIFType[] pattern;

	private boolean subpw;
	
	public Get() {
		format = OutputFormat.BIOPAX; // default
		subpw = false;
	}

	public OutputFormat getFormat() {
		return format;
	}

	public void setFormat(OutputFormat format) {
		this.format = format;
	}

	public String[] getUri() {
		return uri;
	}

	public void setUri(String[] uri) {
		Set<String> uris = new HashSet<>(uri.length);
		for(String item : uri) {
			if(item.contains(",")) {
				//split by ',' ignoring spaces and empty values (between ,,)
				for(String id : item.split("\\s*,\\s*", -1))
					uris.add(id);
			} else {
				uris.add(item.trim());
			}
		}
		this.uri = uris.toArray(new String[uris.size()]);
	}

	//SIF Types
	public SIFType[] getPattern() {
		return pattern;
	}

	public void setPattern(SIFType[] pattern) {
		this.pattern = pattern;
	}

	public boolean getSubpw() {
		return subpw;
	}

	public void setSubpw(boolean subpw) {
		this.subpw = subpw;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString())
			.append(" for:").append(format)
			.append("; spw:").append(subpw)
			.append("; uri:").append(Arrays.toString(uri));
		if(pattern!=null && pattern.length>0)
			sb.append("; pat:").append(Arrays.toString(pattern));

		return sb.toString();
	}

	@Override
	public String cmd() {
		return "get";
	}

	@Override
	public String outputFormat() {
		return format.name().toLowerCase();
	}
}
