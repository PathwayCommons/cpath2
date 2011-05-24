// $Id$
//------------------------------------------------------------------------------
/** Copyright (c) 2009 Memorial Sloan-Kettering Cancer Center.
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
package cpath.warehouse.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.*;
import org.biopax.validator.utils.Normalizer;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;

import cpath.dao.internal.PaxtoolsHibernateDAO;
import cpath.warehouse.WarehouseDAO;

import java.util.*;
import java.io.*;


/**
 * A Paxtools BioPAX Model/DAO to use as a warehouse
 * of molecule and protein references.
 *
 */
@Transactional
@Repository
public class WarehousePaxtoolsHibernateDAO extends PaxtoolsHibernateDAO implements WarehouseDAO
{
	private static final long serialVersionUID = 1L;
	private static Log log = LogFactory.getLog(WarehousePaxtoolsHibernateDAO.class);

	
	@Override
	@Transactional(propagation=Propagation.REQUIRED, readOnly=true)
	public BioPAXElement getObject(String id) 
	{
		if(id == null || "".equals(id)) 
			throw new RuntimeException("getObject(null) is called!");

		BioPAXElement toReturn = null;
		
		// get the element
		BioPAXElement bpe = getByID(id);
		
		// check it has inverse props set ("cloning" may break it...)
		if(bpe instanceof Xref) {
			assert(!((Xref)bpe).getXrefOf().isEmpty());
			if(log.isDebugEnabled()) {
				log.debug(id + " is xrefOf " + 
					((Xref)bpe).getXrefOf().iterator().next().toString()
				);
			}
		}
		
		if (bpe != null) { 
			/* used to be...
			//ElementCloner cloner = new ElementCloner();
			//Model model = cloner.clone(this, bpe);
			*/
			
			// write/read cycle should completely detach the sub-model 
			// (new java objects are created by the simpleIO)
			// and restore inverse properties 
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			exportModel(baos, id);
			Model model = simpleIO.convertFromOWL(new ByteArrayInputStream(baos.toByteArray()));
			toReturn = model.getByID(id);
		}
		
		return toReturn; // null means no such element
	}

	
	@Override
	@Transactional(propagation=Propagation.REQUIRED)
	public Set<BioPAXElement> getObjects()
	{
		return getObjects(BioPAXElement.class);
	}


	/* (non-Javadoc)
	 * @see cpath.warehouse.WarehouseDAO#getObject(java.lang.String, java.lang.Class)
	 */
	@Override
	@Transactional(propagation=Propagation.REQUIRED, readOnly=true)
	public <T extends BioPAXElement> T getObject(String urn, Class<T> clazz) 
	{
		BioPAXElement bpe = getObject(urn);
		if(clazz.isInstance(bpe)) {
			return (T) bpe;
		} else {
			if(bpe != null) log.error("getObject(" +
				urn + ", " + clazz.getSimpleName() + 
				"): returned object has different type, " 
				+ bpe.getModelInterface());
			return null;
		}
	}


	/* (non-Javadoc)
	 * @see cpath.dao.WarehouseDAO#getByXref(java.util.Set, java.lang.Class)
	 * 
	 * xrefs (args) are expected to be "normalized"!
	 * 
	 */
	@Override
	@Transactional(propagation=Propagation.REQUIRED, readOnly=true)
	public Set<String> getByXref(Set<? extends Xref> xrefs, 
		Class<? extends XReferrable> clazz) 
	{
		Set<String> toReturn = new HashSet<String>();
		
		for (Xref xref : xrefs) {			
			// load persistent Xref by ID
			
			/* WARN: x will be always NULL (the BUG was here!)
			 * if cpath2 importer and biopax normalizer 
			 * use different methods to generate xref IDs!
			 * 
			 * The other thing is that the normalizer uses
			 * idVersion property when generating xref IDs,
			 * but we do not want/do this creating our warehouse...
			 */
			//String xurn = xref.getRDFId();
			//Xref x = (Xref) getByID(xurn);
			
			/* this (an alternative) could be used to fix that bug,
			 */
			// map to a normalized RDFId for this type of xref:
			if(xref.getDb() == null || xref.getId() == null) {
				log.warn("getByXref: " + xref + " db or id is null! Skipping.");
				continue;
			}
			
			// generate "normalized" ID, and (unlike it's done in the normalizer) we here 
			// ignore xref.idVersion!!! (TODO think of, e.g., unoprot isoforms, etc. later)
			String xurn = Normalizer.generateURIForXref(xref.getDb(), 
				xref.getId(), null, (Class<? extends Xref>) xref.getModelInterface());
			
			// now try to get it from the warehouse
			Xref x = (Xref) getByID(xurn);
			if (x != null) {
				// collect owners's ids (of requested type only)
				for (XReferrable xr : x.getXrefOf()) {
					if (clazz.isInstance(xr)) {
						toReturn.add(xr.getRDFId());
					}
				}
			} else {
				log.warn("getByXref: using normalized ID:" + xurn 
					+ " " + "no matching xref found for: " +
					xref + " - " + xref.getRDFId() + ". Skipping.");
			}
		}
		
		return toReturn;
	}
	
}


