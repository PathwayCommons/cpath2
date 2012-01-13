package cpath.webservice.args.binding;

import javax.validation.Valid;

import org.biopax.paxtools.query.algorithm.Direction;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import cpath.service.GraphType;
import cpath.webservice.args.Graph;

public class GraphQueryValidator implements Validator {

	@Override
	public boolean supports(Class<?> arg0) {
		return Graph.class.equals(arg0);
	}

	@Override
	public void validate(@Valid Object obj, Errors err) {
		Graph g = (Graph) obj;

		// set defaults
		//if(g.getFormat()==null) { g.setFormat(BIOPAX); } //now - set by OutputFormatEditor
		if(g.getLimit() == null) { g.setLimit(1); } 

		if(g.getDirection() == null)
		{
			g.setDirection(g.getKind() == GraphType.NEIGHBORHOOD ?
				Direction.BOTHSTREAM : Direction.DOWNSTREAM);
		}

		// check
		if (g.getKind() ==  GraphType.COMMONSTREAM && g.getDirection() == Direction.BOTHSTREAM) {
			err.rejectValue("direction", "COMMONSTREAM.direction", 
				"Direction parameter cannot be " + g.getDirection() + " for COMMONSTREAM query!");
		}
		
		if (g.getLimit() < 0)
		{
			err.rejectValue("limit", "COMMONSTREAM.limit", 
				"Search limit must be specified and must be non-negative");
		}
		
	}
}
