package cpath.analysis;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import cpath.service.ConsoleApplication;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.BindingFeature;
import org.biopax.paxtools.model.level3.EntityFeature;
import org.biopax.paxtools.model.level3.ExperimentalForm;
import org.biopax.paxtools.model.level3.ModificationFeature;
import org.biopax.paxtools.model.level3.PhysicalEntity;
import org.biopax.paxtools.model.level3.Provenance;

import cpath.service.api.Analysis;

/**
 * Prints out а summary of the main merged BioPAX model
 * (just for development and debugging).
 * 
 * This can be used with {@link ConsoleApplication} '--analyze' ('-a') command.
 * 
 * This uses a Java option: cpath.analysis.filter.metadataids=id1,id2,.. 
 * if not defined, all data are analyzed; id1,id2,.. are datasource identifiers (see: metadata.json).
 * 
 * @author rodche
 */
public final class EntityFeaturesSummary implements Analysis<Model> {
	
	public static final String JAVA_OPTION_DATASOURCES = "cpath.analysis.filter.metadataids";
	

	public void execute(Model model) {
		
		final Set<String> providerURIs = new HashSet<>();
		final String javaPropertyDatasources = System.getProperty(JAVA_OPTION_DATASOURCES);
		if(javaPropertyDatasources!=null && !javaPropertyDatasources.isEmpty())
			for(String id : javaPropertyDatasources.split(","))
				providerURIs.add(model.getXmlBase()+id);
		
		final Collection<ExperimentalForm> allExpForms = model.getObjects(ExperimentalForm.class);
				
		for(EntityFeature ef : model.getObjects(EntityFeature.class)) {
			Set<String> providers = new HashSet<>();
			Set<String> eforms = new HashSet<>();
			Set<PhysicalEntity> pes = new HashSet<>();
			pes.addAll(ef.getFeatureOf());
			pes.addAll(ef.getNotFeatureOf());
			
			boolean passedProvidersFilter = (providerURIs.isEmpty())? true : false;
			for(PhysicalEntity pe : pes) {
				for(Provenance ds : pe.getDataSource()) {
					providers.add(ds.getDisplayName());
					if(providerURIs.contains(ds.getUri()))
						passedProvidersFilter = true;
				}
			}
			if(!passedProvidersFilter && !providers.isEmpty()) 
				continue;
			
			//cannot easily get owner ExperimentalForm objects (no experimentalFeatureOf property exists...)
			for(ExperimentalForm xf : allExpForms) {
				if(xf.getExperimentalFeature().contains(ef))
					eforms.add(xf.getUri());
			}	
			
			//output			
			System.out.printf("%s\t%s\tpe providers: %s\tlocation: %s ", 
					ef.getModelInterface().getSimpleName(), ef.getUri(),
					providers, ef.getFeatureLocation(), ef.getFeatureLocationType());
			
			if(ef.getFeatureLocationType() != null)
				System.out.printf("%s", ef.getFeatureLocationType().getXref());
			
			System.out.print("\t");
			
			if(!eforms.isEmpty())
				System.out.printf("exp. forms: %s\t", eforms);
			
			if(ef instanceof BindingFeature) {
				BindingFeature bf = (BindingFeature) ef;
				System.out.printf("bindsTo: %s\t", bf.getBindsTo());
			}
			
			if(ef instanceof ModificationFeature) {
				ModificationFeature mf = (ModificationFeature) ef;
				System.out.printf("modif. type: %s\t", mf.getModificationType());
				if(mf.getModificationType() != null)
					System.out.printf("%s\t", mf.getModificationType().getXref());
			}
			
			System.out.println();
		}
	}

}
