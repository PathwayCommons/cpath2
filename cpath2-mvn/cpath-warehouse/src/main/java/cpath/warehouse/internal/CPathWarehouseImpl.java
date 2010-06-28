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

package cpath.warehouse.internal;

// imports
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.model.level3.ControlledVocabulary;
import org.biopax.paxtools.model.level3.ProteinReference;
import org.biopax.paxtools.model.level3.SmallMoleculeReference;
import org.biopax.paxtools.model.level3.UtilityClass;
import org.biopax.paxtools.model.level3.Xref;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cpath.dao.PaxtoolsDAO;
import cpath.warehouse.CPathWarehouse;
import cpath.warehouse.CvRepository;

import java.util.List;
import java.util.Set;

/**
 * @author rodch
 *
 */
@Service
public final class CPathWarehouseImpl implements CPathWarehouse {
	private final static Log log = LogFactory.getLog(CPathWarehouseImpl.class);
	
    private CvRepository cvRepository;
    private PaxtoolsDAO moleculesDAO;
    private PaxtoolsDAO proteinsDAO;
	
	
	public CPathWarehouseImpl(PaxtoolsDAO moleculesDAO, 
			CvRepository cvRepository, PaxtoolsDAO proteinsDAO) 
	{
		this.cvRepository = cvRepository;
		this.moleculesDAO = moleculesDAO;
		this.proteinsDAO = proteinsDAO;
	}

	

	/* (non-Javadoc)
	 * @see cpath.warehouse.CPathWarehouse#getObject(java.lang.String, java.lang.Class)
	 */
	@Override
	@Transactional
	public <T extends UtilityClass> T getObject(String primaryUrn,
			Class<T> utilityClazz) 
	{
		if(SmallMoleculeReference.class.isAssignableFrom(utilityClazz)) 
		{
			T bpe = (T) moleculesDAO.getByID(primaryUrn);
			return bpe; // TODO clone - completely detach (create a new one)
		} else if(ProteinReference.class.isAssignableFrom(utilityClazz)) 
		{
			T bpe = (T) proteinsDAO.getByID(primaryUrn);
			return bpe; // TODO clone
		} else if(ControlledVocabulary.class.isAssignableFrom(utilityClazz)) 
		{
			return (T) cvRepository.getControlledVocabulary(primaryUrn, 
					(Class<ControlledVocabulary>)utilityClazz);
		}
		
		return null;
	}


	/* (non-Javadoc)
	 * @see cpath.warehouse.CPathWarehouse#getObject(java.util.Set, java.lang.Class)
	 */
	@Override
	@Transactional
	public <T extends UtilityClass> T getObject(Set<? extends Xref> xrefs,
			Class<T> utilityClazz) 
	{
		for(Xref xref : xrefs) {
			if(SmallMoleculeReference.class.isAssignableFrom(utilityClazz)) 
			{
				List<T> mols = moleculesDAO.search(xref.getId(), utilityClazz);
				// honestly expected ONE or none in the list!
				if(!mols.isEmpty())
					return mols.iterator().next();
				
			} else if(ProteinReference.class.isAssignableFrom(utilityClazz)) 
			{
				List<T> prs = proteinsDAO.search(xref.getId(), utilityClazz);
				// expected ONE or none!
				if(!prs.isEmpty())
					return prs.iterator().next();
				
			} else if(ControlledVocabulary.class.isAssignableFrom(utilityClazz)) 
			{
				T cv = (T) cvRepository.getControlledVocabulary(xref.getDb(), xref.getId(), 
						(Class<ControlledVocabulary>)utilityClazz);
				if(cv != null)
					return cv;
			}
		}
		
		return null;
	}

	
	@Override
	public Set<String> getAllChildren(String urn) {
		return cvRepository.getAllChildren(urn);
	}

	
	@Override
	public Set<String> getDirectChildren(String urn) {
		return cvRepository.getDirectChildren(urn);
	}

	
	@Override
	public Set<String> getAllParents(String urn) {
		return cvRepository.getAllParents(urn);
	}

	
	@Override
	public Set<String> getDirectParents(String urn) {
		return cvRepository.getDirectParents(urn);
	}



	/* (non-Javadoc)
	 * @see cpath.warehouse.CPathWarehouse#getPrimaryURI(java.lang.String, java.lang.Class)
	 */
	@Override
	public String getPrimaryURI(String id,
			Class<? extends UtilityClass> utilityClass) {
		// TODO Auto-generated method stub
		return null;
	}



	/* (non-Javadoc)
	 * @see cpath.warehouse.CvRepository#getControlledVocabulary(java.lang.String, java.lang.Class)
	 */
	@Override
	public <T extends ControlledVocabulary> T getControlledVocabulary(
			String urn, Class<T> cvClass) {
		return cvRepository.getControlledVocabulary(urn, cvClass);
	}



	/* (non-Javadoc)
	 * @see cpath.warehouse.CvRepository#getControlledVocabulary(java.lang.String, java.lang.String, java.lang.Class)
	 */
	@Override
	public <T extends ControlledVocabulary> T getControlledVocabulary(
			String db, String id, Class<T> cvClass) {
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



	/* (non-Javadoc)
	 * @see cpath.warehouse.CPathWarehouse#createIndex()
	 */
	@Override
	public void createIndex() {
		proteinsDAO.createIndex();
		moleculesDAO.createIndex();
	}
	
}
