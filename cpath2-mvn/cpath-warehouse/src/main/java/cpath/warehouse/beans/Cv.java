/**
 ** Copyright (c) 2009 Memorial Sloan-Kettering Cancer Center (MSKCC)
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

package cpath.warehouse.beans;

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Transient;

import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.level3.ControlledVocabulary;
import org.biopax.paxtools.proxy.level3.ControlledVocabularyProxy;
import org.hibernate.search.annotations.Indexed;

/**
 * Managed by CPathWarehouse BioPAX Controlled Vocabulary
 * (CV's hierarchy is also available)
 * 
 * @author rodch
 *
 */
@Entity(name = "cv")
@Indexed(index = "cv")
public class Cv extends ControlledVocabularyProxy {

	private Set<Cv> members;
	
	public Cv() {}

	@ManyToMany(cascade = {CascadeType.ALL}, targetEntity = Cv.class)
	@JoinTable(name = "cv_relationship")
	public Set<Cv> getMembers() {
		return members;
	}

	public void setMembers(Set<Cv> members) {
		this.members = members;
	}

	public void addMember(Cv member) {
		this.members.add(member);
	}
	
	public void removeMember(Cv member) {
		this.members.remove(member);
	}
	
	/**
	 * Creates an "ordinary", non-persistent,
	 * BioPAX Controlled Vocabulary copy
	 * 
	 * @param <T> extends ControlledVocabulary
	 * @return
	 */
	@Transient
	public <T extends ControlledVocabulary> T clone() {
		T cv = (T) BioPAXLevel.L3.getDefaultFactory().reflectivelyCreate(this.getModelInterface());
		cv.setRDFId(getRDFId());
		cv.setTerm(getTerm());
		cv.setXref(getXref());
		return cv;
	}
	
}
