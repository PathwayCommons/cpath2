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

package cpath.warehouse.cv.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;

import cpath.warehouse.beans.Cv;
import cpath.warehouse.cv.CvRepository;

/**
 * @author rodch
 *
 */
@Repository
public class CvHibernateRepository implements CvRepository {

    // log
    private static Log log = LogFactory.getLog(CvHibernateRepository.class);

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
	 * @see cpath.warehouse.cv.CvRepository#addCV(cpath.warehouse.beans.Cv)
	 */
	@Override
	public void addCV(Cv cv) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see cpath.warehouse.cv.CvRepository#removeAllCVs()
	 */
	@Override
	public void removeAllCVs() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see cpath.warehouse.cv.CvRepository#removeCV(cpath.warehouse.beans.Cv)
	 */
	@Override
	public void removeCV(Cv cv) {
		// TODO Auto-generated method stub

	}


	/* (non-Javadoc)
	 * @see cpath.warehouse.cv.CvRepository#getByRDFId(java.lang.String)
	 */
	@Override
	public Cv getByRDFId(String urn) {
		// TODO Auto-generated method stub
		return null;
	}

}
