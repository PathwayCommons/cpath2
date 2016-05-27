import org.apache.commons.lang.StringUtils;
import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Conversion;
import org.biopax.paxtools.model.level3.PhysicalEntity;
import org.biopax.paxtools.model.level3.Protein;
import org.biopax.paxtools.util.EquivalenceGrouper;
import org.junit.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class QuickTest {
    @Test
    public void splitSliceJoin() {
        String cols[] = "A\tB\tC\tD\tE".split("\t", 4);
        assertEquals(4, cols.length); //A, B, C and "D\tE"
        //join columns 0, 1, 2 -
        String res = StringUtils.join(Arrays.copyOfRange(cols, 0, 3), '\t');
        assertEquals("A\tB\tC", res);
        //System.out.println(String.format("%3.1f",35.6345f));

        Matcher matcher = Pattern.compile("([a-zA-Z0-9\\. ]+)\\s*\\(\\s*(\\d+)\\s*\\)").matcher("Homo sapiens (9606)");
        assertTrue(matcher.find());
        assertEquals(2, matcher.groupCount());
//        System.out.println(String.format("%s - %s",matcher.group(1),matcher.group(2)));
    }

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
