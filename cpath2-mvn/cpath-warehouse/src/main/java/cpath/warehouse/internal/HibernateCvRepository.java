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

package cpath.warehouse.internal;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import cpath.warehouse.CvRepository;
import cpath.warehouse.beans.Cv;

/**
 * @author rodch
 * 
 *TODO all fetched BioPAX CVs can be managed via this repository or  use a non-hibernate in-memory repository that extends OntologyManagerAdapter directly?
 *
 */
@Repository
public class HibernateCvRepository implements CvRepository {
    private static Log log = LogFactory.getLog(HibernateCvRepository.class);

	// session factory prop/methods used by spring
	private SessionFactory sessionFactory;
	
	
	public SessionFactory getSessionFactory() { return sessionFactory; }
	
	
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	// a shortcut to get current session
	private Session getSession() {
		return getSessionFactory().getCurrentSession();
	}


	/* (non-Javadoc)
	 * @see cpath.warehouse.CvRepository#add(cpath.warehouse.beans.Cv)
	 */
	@Transactional
	public void add(Cv cv) {
		getSession().save(cv);
	}

	
	/* (non-Javadoc)
	 * @see cpath.warehouse.CvRepository#getAllChildren(java.lang.String)
	 */
	@Override
	public Set<String> getAllChildren(String urn) {
		// TODO Auto-generated method stub
		return null;
	}


	/* (non-Javadoc)
	 * @see cpath.warehouse.CvRepository#getAllParents(java.lang.String)
	 */
	@Override
	public Set<String> getAllParents(String urn) {
		// TODO Auto-generated method stub
		return null;
	}


	/* (non-Javadoc)
	 * @see cpath.warehouse.CvRepository#getById(java.lang.String)
	 */
	@Override
	public Cv getById(String urn) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see cpath.warehouse.CvRepository#getDirectChildren(java.lang.String)
	 */
	@Override
	public Set<String> getDirectChildren(String urn) {
		// TODO Auto-generated method stub
		return null;
	}


	/* (non-Javadoc)
	 * @see cpath.warehouse.CvRepository#getDirectParents(java.lang.String)
	 */
	@Override
	public Set<String> getDirectParents(String urn) {
		// TODO Auto-generated method stub
		return null;
	}


	/* (non-Javadoc)
	 * @see cpath.warehouse.CvRepository#isChild(java.lang.String, java.lang.String)
	 */
	@Override
	public boolean isChild(String parentUrn, String urn) {
		// TODO Auto-generated method stub
		return false;
	}
	

}
