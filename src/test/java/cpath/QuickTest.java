package cpath;

import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.PhysicalEntity;
import org.biopax.paxtools.model.level3.Protein;
import org.biopax.paxtools.util.EquivalenceGrouper;
import org.junit.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.*;

import static org.junit.Assert.*;

public class QuickTest {

    //Having Evidence prevents otherwise equivalent proteins from merging
    //by using ModelUtils.mergeEquivalentPhysicalEntities(model) utility...
    @Test
    public void mergeEquivPEs() throws Exception {
        SimpleIOHandler reader = new SimpleIOHandler();
        Model model = reader.convertFromOWL((new DefaultResourceLoader())
                .getResource("classpath:test_merge_equiv_pe.owl").getInputStream());

        for(Protein p : model.getObjects(Protein.class))
        System.out.println(p.getUri()+" has equiv. code=" + p.equivalenceCode());

        EquivalenceGrouper<PhysicalEntity> groups = new EquivalenceGrouper(model.getObjects(PhysicalEntity.class));
        Set<? extends List> buckets = groups.getBuckets();
        assertEquals(3, buckets.size());
        //despite all three Ps have the same equivalenceCode(),
        //isEquivalent method for any pair returns false (because of having different Evidence objects)

        ModelUtils.mergeEquivalentPhysicalEntities(model);
        assertEquals(3, model.getObjects(Protein.class).size());

        //remove evidence
        for(Protein p : model.getObjects(Protein.class)) {
            p.getEvidence().clear();
        }

        //and now three Proteins will become one
        ModelUtils.mergeEquivalentPhysicalEntities(model);
        assertEquals(1, model.getObjects(Protein.class).size());
    }

}
