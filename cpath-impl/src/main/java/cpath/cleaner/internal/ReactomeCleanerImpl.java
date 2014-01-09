package cpath.cleaner.internal;

//import org.biopax.paxtools.model.*;
//import org.biopax.paxtools.model.level3.Entity;
//import org.biopax.paxtools.model.level3.UnificationXref;
//import org.biopax.paxtools.model.level3.Xref;
//import org.biopax.paxtools.util.ClassFilterSet;
//import org.biopax.paxtools.io.*;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Conversion;
import org.biopax.paxtools.model.level3.ConversionDirectionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import cpath.dao.CPathUtils;
import cpath.importer.Cleaner;

//import java.io.*;
//import java.util.HashSet;
//import java.util.Set;

/**
 * Implementation of Cleaner interface for Reactome data. 
 * 
 * Can normalize URIs for Reactome Entity class objects 
 * to http://identifiers.org/reactome/REACT_* form
 * if the unification xref with the stable Reactome ID is attached. 
 * TODO ? add/remove features as needed, as reactome versions change...
 */
final class ReactomeCleanerImpl implements Cleaner {
	
	// logger
    private static Logger log = LoggerFactory.getLogger(ReactomeCleanerImpl.class);

    @Override
	public String clean(final String pathwayData) 
	{	
		// create bp model from pathwayData
		InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(pathwayData.getBytes()));
		SimpleIOHandler simpleReader = new SimpleIOHandler(BioPAXLevel.L3);
		Model model = simpleReader.convertFromOWL(inputStream);
		log.info("Cleaning Reactome data, this may take some time, please be patient...");
//		
//		//normalize entity URIs using Reactome stable id, where possible		
//		Set<Entity> entities = new HashSet<Entity>(model.getObjects(Entity.class));
//		for(Entity ent : entities) {
//			Set<UnificationXref> uxrefs = 
//				new ClassFilterSet<Xref, UnificationXref>(ent.getXref(), UnificationXref.class);			
//			for(UnificationXref x : uxrefs)
//				if(x.getId() != null && x.getId().startsWith("REACT_"))
//					CPathUtils.replaceID(model, ent, "http://identifiers.org/reactome/" + x.getId());
//		}		
//		
		
		// All Conversions in Reactome are LEFT-TO-RIGH, unless otherwise was specified (confirmed with Guanming Wu, 2013/12)
		Set<Conversion> conversions = new HashSet<Conversion>(model.getObjects(Conversion.class));
		for(Conversion ent : conversions) {
			if(ent.getConversionDirection() == null)
				ent.setConversionDirection(ConversionDirectionType.LEFT_TO_RIGHT);
		}
		
		
		// convert model back to OutputStream for return
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			simpleReader.convertToOWL(model, outputStream);
		} catch (Exception e) {
			throw new RuntimeException("clean(), Exception thrown while cleaning pathway data", e);
		}

		return outputStream.toString();
		
	}

}
