package cpath.webservice.validation.protocol;

import java.util.HashSet;
import java.util.Set;

/**
 * Protocol Constants, Version 2.0.
 *
 * @author cPath Dev Team.
 */
public class ProtocolConstantsVersion2 {
    /**
     * HashSet of Valid Commands.
     */
    private static Set<String> validCommands;

    /**
     * Get Neighbors Command.
     */
    public static final String COMMAND_GET_NEIGHBORS = "get_neighbors";

    /**
     * Get Pathway List Command.
     */
    public static final String COMMAND_GET_PATHWAY_LIST = "get_pathways";

    /**
     * Get Pathway List Command.
     */
    public static final String COMMAND_GET_PARENT_SUMMARIES = "get_parents";

    /**
     * Search by Keyword.
     */
    public static final String COMMAND_SEARCH = "search";    

    /**
     * ID_LIST Format / Output.
     */
    public static final String FORMAT_ID_LIST = "id_list";

    /**
     * BINARY_SIF Format / Output.
     */
    public static final String FORMAT_BINARY_SIF = "binary_sif";

    /**
     * FORMAT_GSEA_ENTREZ_GENE_ID.
     */
    public static final String FORMAT_GSEA = "gsea";

    /**
     * FORMAT_PC_OUTPUT
     */
    public static final String FORMAT_PC_GENE_SET = "pc_gene_set";

    /**
     * IMAGE_MAP Format / Output.
     */
    public static final String FORMAT_IMAGE_MAP = "image_map";

    /**
     * IMAGE_MAP_THUMBNAIL Format / Output.
     */
    public static final String FORMAT_IMAGE_MAP_THUMBNAIL = "image_map_thumbnail";

    /**
     * IMAGE_MAP_IPHONE Format / Output.
     */
    public static final String FORMAT_IMAGE_MAP_IPHONE = "image_map_iphone";

    /**
     * IMAGE_MAP_FRAMESET Format / Output.
     */
    public static final String FORMAT_IMAGE_MAP_FRAMESET = "image_map_frameset";

    /**
     * Protocol Version:  2.0
     */
    public static final String VERSION_2 = "2.0";

    /**
     * Max Number of Input IDs that can be specified.
     */
    public static final int MAX_NUM_IDS = 25;

    /**
     * Gets HashSet of Valid Commands.
     *
     * @return HashMap of Valid Commands.
     */
    public Set<String> getValidCommands() {
        if (validCommands == null) {
            validCommands = new HashSet<String>();
            validCommands.add(ProtocolConstants.COMMAND_GET_RECORD_BY_CPATH_ID);
            validCommands.add(ProtocolConstants.COMMAND_HELP);
            validCommands.add(ProtocolConstants.COMMAND_GET_BY_KEYWORD);
            validCommands.add(COMMAND_SEARCH);
            validCommands.add(COMMAND_GET_NEIGHBORS);
            validCommands.add(COMMAND_GET_PATHWAY_LIST);
            validCommands.add(COMMAND_GET_PARENT_SUMMARIES);
        }
        return validCommands;
    }
}