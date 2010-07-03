package cpath.converter.internal;
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


import static org.junit.Assert.*;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.biopax.paxtools.controller.SimpleMerger;
import org.biopax.paxtools.impl.ModelImpl;
import org.biopax.paxtools.io.simpleIO.SimpleEditorMap;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.ProteinReference;
import org.junit.Test;

import cpath.converter.Converter;

/**
 * @author rodche
 *
 */
public class UniprotConverterImplTest {

	/**
	 * Test method for {@link cpath.converter.internal.UniprotConverterImpl#convert(java.io.InputStream, org.biopax.paxtools.model.Model)}.
	 * @throws IOException 
	 */
	@Test
	public void testConvert() throws IOException {
		Converter converter = new UniprotConverterImpl();
		InputStream is = getClass().getResourceAsStream("/test_uniprot_data.dat.gz");
		GZIPInputStream zis = new GZIPInputStream(new BufferedInputStream(is));
		
		// extend Model for the converter the calls 'merge' method to work
		Model model = new ModelImpl(BioPAXLevel.L3.getDefaultFactory()) {
			/* (non-Javadoc)
			 * @see org.biopax.paxtools.impl.ModelImpl#merge(org.biopax.paxtools.model.Model)
			 */
			@Override
			public void merge(Model source) {
				SimpleMerger simpleMerger = new SimpleMerger(new SimpleEditorMap(getLevel()));
				simpleMerger.merge(this, source);
			}
		};
		
		
		converter.convert(zis, model);
		
		Set<ProteinReference> proteinReferences = model.getObjects(ProteinReference.class);
		assertTrue(proteinReferences.size()==6);
		assertTrue(proteinReferences.iterator().next().getXref().iterator().hasNext());
		
		//TODO add more checks that the conversion went ok...
		//(new SimpleExporter(BioPAXLevel.L3)).convertToOWL(model, System.out);
	}

}
