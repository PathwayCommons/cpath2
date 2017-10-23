package cpath.service.args.binding;

import org.biopax.paxtools.pattern.miner.SIFEnum;
import org.biopax.paxtools.pattern.miner.SIFType;

import java.beans.PropertyEditorSupport;


/**
 * @author rodche
 *
 */
public class SIFTypeEditor extends PropertyEditorSupport {
	
	/* (non-Javadoc)
	 * @see java.beans.PropertyEditorSupport#setAsText(java.lang.String)
	 */
	@Override
	public void setAsText(String arg0) {
		setValue(SIFEnum.typeOf(arg0.trim()));
	}
	
}
