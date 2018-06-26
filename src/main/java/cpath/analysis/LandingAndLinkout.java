package cpath.analysis;

import cpath.service.ConsoleConfiguration;
import cpath.service.api.Analysis;
import cpath.service.api.CPathService;
import cpath.service.CPathUtils;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.RelationshipXref;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.model.level3.Xref;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class LandingAndLinkout implements Analysis<Model> {

    private static final Logger LOG = LoggerFactory.getLogger(LandingAndLinkout.class);
//    public static final String JAVA_OPTION_IDTYPE = "cpath.analysis.linkout.type";

    @Override
    public void execute(Model model) {
        ConfigurableApplicationContext context = SpringApplication.run(ConsoleConfiguration.class);
        try {
            final CPathService service = context.getBean(CPathService.class);
            service.init();
            //export the list of unique UniProt primary accession numbers
            LOG.info("creating the list of primary uniprot IDs...");
            Set<String> acs = new TreeSet<>();
            //exclude publication xrefs
            Set<Xref> xrefs = new HashSet<>(model.getObjects(UnificationXref.class));
            xrefs.addAll(model.getObjects(RelationshipXref.class));
            long left = xrefs.size();
            for (Xref x : xrefs) {
                String id = x.getId();
                if (CPathUtils.startsWithAnyIgnoreCase(x.getDb(), "uniprot")
                        && id != null && !acs.contains(id))
                    acs.addAll(service.map(id, "UNIPROT"));
                if (--left % 10000 == 0)
                    LOG.info(left + " xrefs to map...");
            }

            PrintWriter writer;
            try {
                writer = new PrintWriter("uniprot.txt");
            } catch (FileNotFoundException e) {
                throw new RuntimeException("", e);
            }

            writer.println(String.format("#PathwayCommons v%s - primary UniProt accession numbers:",
                service.settings().getVersion()));

            for (String ac : acs)
                writer.println(ac);

            writer.close();

        } finally {
            context.close();
        }

        LOG.info("generated uniprot.txt");
    }
}
