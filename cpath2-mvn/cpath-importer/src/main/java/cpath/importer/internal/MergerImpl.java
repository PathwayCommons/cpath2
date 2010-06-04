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

package cpath.importer.internal;

// imports
import cpath.importer.Merger;
import cpath.dao.PaxtoolsDAO;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.internal.DataSourceFactory;
import cpath.warehouse.CPathWarehouse;
import cpath.warehouse.MetadataDAO;
import cpath.warehouse.PathwayDataJDBCServices;

import org.biopax.paxtools.io.simpleIO.SimpleEditorMap;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.level3.SmallMoleculeReference;
import org.biopax.paxtools.model.level3.ProteinReference;
import org.biopax.paxtools.model.level3.ControlledVocabulary;
import org.biopax.paxtools.model.level3.UtilityClass;
import org.biopax.paxtools.controller.SimpleMerger;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Set;

import javax.sql.DataSource;


/**
 * Class responsible for Merging pathway data.
 */
public final class MergerImpl implements Merger {

	// log
    private static final Log log = LogFactory.getLog(MergerImpl.class);

	// ref to MetadataDAO
	private MetadataDAO metadataDAO;

	// ref to pc repository
	private PaxtoolsDAO pcDAO;

	// ref to the warehouse
	private CPathWarehouse cpathWarehouse;
	
	// ref to jdbc services
	private PathwayDataJDBCServices pathwayDataJDBCServices;


	/**
	 *
	 * Constructor.
	 *
	 * @param pcDAO PaxtoolsDAO;
     * @param metadataDAO MetadataDAO
	 * @param cPathWarehouse CPathWarehouse
	 * @param pathwaydDataJDBCServices PathwayDataJDBCServices
	 */
	public MergerImpl(final PaxtoolsDAO pcDAO,
					  final MetadataDAO metadataDAO,
					  final CPathWarehouse cpathWarehouse,
					  final PathwayDataJDBCServices pathwayDataJDBCServices) 
	{
		// init members
		this.pcDAO = pcDAO;
		this.metadataDAO = metadataDAO;
		this.cpathWarehouse = cpathWarehouse;
		this.pathwayDataJDBCServices = pathwayDataJDBCServices;
	}

    /*
	 * (non-Javadoc)
	 * @see cpath.importer.Merger#merge
	 */
	@Override
	public void merge() {
		SimpleMerger simpleMerger = new SimpleMerger(new SimpleEditorMap(BioPAXLevel.L3));

		// create pc model
		Model pcModel= BioPAXLevel.L3.getDefaultFactory().createModel();

		// iterate over all providers
		for (Metadata metadata : metadataDAO.getAll()) {

			// get the persisted model for this provider
			Model pathwayModel = getModel(metadata);

			// iterate over all elements in pathway model
			for (BioPAXElement bpe : pathwayModel.getObjects()) {

				// check if element already exists in pc
				boolean elementExistsInPC = (pcModel.getByID(bpe.getRDFId()) != null);

				// merge all protein/SM references & controlled vocabularies
				if ((bpe instanceof ProteinReference ||
					 bpe instanceof SmallMoleculeReference ||
					 bpe instanceof ControlledVocabulary)
					&& !elementExistsInPC) 
				{
					// find specific subclass (e.g. CellVocabulary)!
					Class<? extends UtilityClass> cvType = 
						(Class<? extends UtilityClass>) bpe.getModelInterface(); 
					UtilityClass object = cpathWarehouse
						.getObject(bpe.getRDFId(), cvType);
					pcModel.add(object);
				}
			}
			
			// local merge
			simpleMerger.merge(pcModel, pathwayModel);
		}
		
		// final merge
		//pcDAO.importModel(pcModel);
		simpleMerger.merge(pcDAO, pcModel);
	}

	/**
	 * For the given provider, gets the persisted model.
	 *
	 * @param metadata Metadata
	 * @return Model
	 */
	private Model getModel(final Metadata metadata) {
		// create data source
		DataSource pathwayDataSource = pathwayDataJDBCServices
			.getDataSource(metadata.getIdentifier());

		// get application context after setting the new, custom, datasource (replaces former one)
		DataSourceFactory.getDataSourceMap().put("mergeDataSource", pathwayDataSource);
		
		ApplicationContext context = 
			new ClassPathXmlApplicationContext("classpath:internalContext-merge.xml");
		// get a ref to PaxtoolsDAO
		PaxtoolsDAO mergePaxtoolsDAO = (PaxtoolsDAO)context.getBean("mergePaxtoolsDAO");

		// create a model, grab all elements from db and add to model
		Model toReturn = BioPAXLevel.L3.getDefaultFactory().createModel();
		
		Set<BioPAXElement> allElements = mergePaxtoolsDAO.getElements(BioPAXElement.class, true);
		for (BioPAXElement bpe : allElements) {
			toReturn.add(bpe);
		}

		// outta here
		return toReturn;
	}
}