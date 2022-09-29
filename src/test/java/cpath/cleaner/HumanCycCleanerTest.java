package cpath.cleaner;

import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Protein;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HumanCycCleanerTest {
    @Test
    public void deleteHtmlFromNames() throws Exception
    {
        HumanCycCleaner humanCycCleaner = new HumanCycCleaner();

        assertEquals("Protein",humanCycCleaner.deleteHtmlFromName("Protein"));
        assertEquals("beta-Protein",humanCycCleaner.deleteHtmlFromName("<i>&beta;-Protein</i>"));
        assertEquals("Protein A beta-chain",humanCycCleaner.deleteHtmlFromName("Protein A &amp;beta;-chain"));

        Model model = BioPAXLevel.L3.getDefaultFactory().createModel();
        Protein p = model.addNew(Protein.class,"protein");
        p.setDisplayName("Protein");
        p.setStandardName("<i>&beta;-Protein</i>");
        p.addName("Protein A &amp;beta;-chain");

        humanCycCleaner.deleteHtmlFromNames(model);

        assertEquals("Protein",p.getDisplayName());
        assertEquals("beta-Protein",p.getStandardName());
        assertTrue(p.getName().contains("Protein A beta-chain"));
        assertFalse(p.getName().contains("Protein A &amp;beta;-chain"));
        assertFalse(p.getName().contains("<i>&beta;-Protein</i>"));
    }
}