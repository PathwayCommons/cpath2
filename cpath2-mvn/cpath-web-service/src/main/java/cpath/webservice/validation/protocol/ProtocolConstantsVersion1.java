// $Id: ProtocolConstantsVersion1.java,v 1.5 2007/09/04 17:39:08 cerami Exp $
//------------------------------------------------------------------------------
/** Copyright (c) 2006 Memorial Sloan-Kettering Cancer Center.
 **
 ** Code written by: Ethan Cerami
 ** Authors: Ethan Cerami, Gary Bader, Chris Sander
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
package cpath.webservice.validation.protocol;

import java.util.HashSet;

/**
 * Protocol Constants, Version 1.0.
 *
 * @author cPath Dev Team.
 */
public class ProtocolConstantsVersion1 {
    /**
     * HashSet of Valid PSI-MI Commands.
     */
    private static HashSet validPsiMiCommands;

    /**
     * HashSet of Valid BioPAX Commands.
     */
    private static HashSet validBioPaxCommands;

    /**
     * HashMap of Valid PSI-MI Formats.
     */
    private static HashSet validPsiMiFormats;

    /**
     * HashMap of Valid BioPax Formats.
     */
    private static HashSet validBioPaxFormats;

    /**
     * XML Format.
     */
    public static final String FORMAT_XML = "xml";

    /**
     * PSI-MI XML Format.
     */
    public static final String FORMAT_PSI_MI = "psi_mi";

    /**
     * BioPAX Format.
     */
    public static final String FORMAT_BIO_PAX = "biopax";

     /**
     * Count Only Format.
     */
    public static final String FORMAT_COUNT_ONLY = "count_only";

    /**
     * Protocol Version:  1.0
     */
    public static final String VERSION_1 = "1.0";

    /**
     * Get By Interactor Name.
     */
    public static final String COMMAND_GET_BY_INTERACTOR_NAME_XREF =
            "get_by_interactor_name_xref";

    /**
     * Get By Interactor ID.
     */
    public static final String COMMAND_GET_BY_INTERACTOR_ID =
            "get_by_interactor_id";

    /**
    /**
     * Get Top-Level Pathway List.
     */
    public static final String COMMAND_GET_TOP_LEVEL_PATHWAY_LIST =
            "get_top_level_pathway_list";

    /**
     * Get By Interactor Taxonomy ID.
     */
    public static final String COMMAND_GET_BY_ORGANISM = "get_by_organism";


    /**
     * Get By Interaction Database.
     */
    public static final String COMMAND_GET_BY_DATABASE =
            "get_by_database";

    /**
     * Get By Interaction PubMedId.
     */
    public static final String COMMAND_GET_BY_PMID =
            "get_by_pmid";

    /**
     * Get By Experiment Type.
     */
    public static final String COMMAND_GET_BY_EXPERIMENT_TYPE =
            "get_by_experiment_type";

    /**
     * Get Organism List.
     */
    public static final String COMMAND_GET_ORGANISM_LIST =
            "get_organism_list";

    /**
     * Default Max Number of Hits.
     */
    public static final int DEFAULT_MAX_HITS = 10;

    /**
     * Maximum Number of Hits you can Request at Once.
     */
    public static final int MAX_NUM_HITS = 1000;

    /**
     * Gets HashSet of Valid PSI-MI Commands.
     *
     * @return HashMap of Valid Commands.
     */
    public HashSet getValidPsiMiCommands() {
        if (validPsiMiCommands == null) {
            validPsiMiCommands = new HashSet();
            validPsiMiCommands.add(COMMAND_GET_BY_INTERACTOR_NAME_XREF);
            validPsiMiCommands.add(COMMAND_GET_BY_INTERACTOR_ID);
            validPsiMiCommands.add(COMMAND_GET_BY_ORGANISM);
            validPsiMiCommands.add(ProtocolConstants.COMMAND_GET_BY_KEYWORD);
            validPsiMiCommands.add(COMMAND_GET_BY_DATABASE);
            validPsiMiCommands.add(COMMAND_GET_BY_PMID);
            validPsiMiCommands.add(COMMAND_GET_BY_EXPERIMENT_TYPE);
            validPsiMiCommands.add(ProtocolConstants.COMMAND_HELP);
        }
        return validPsiMiCommands;
    }

    /**
     * Gets HashSet of Valid BioPAX Commands.
     *
     * @return HashMap of Valid Commands.
     */
    public HashSet getValidBioPaxCommands() {
        if (validBioPaxCommands == null) {
            validBioPaxCommands = new HashSet();
            validBioPaxCommands.add(ProtocolConstants.COMMAND_HELP);
            validBioPaxCommands.add(ProtocolConstants.COMMAND_GET_BY_KEYWORD);
            validBioPaxCommands.add(ProtocolConstants.COMMAND_GET_RECORD_BY_CPATH_ID);
            validBioPaxCommands.add(COMMAND_GET_TOP_LEVEL_PATHWAY_LIST);
        }
        return validBioPaxCommands;
    }


    /**
     * Gets HashMap of Valid PSI-MI Formats.
     *
     * @return HashMap of Valid Formats.
     */
    public HashSet getValidPsiMiFormats() {
        if (validPsiMiFormats == null) {
            validPsiMiFormats = new HashSet();
            validPsiMiFormats.add(FORMAT_XML);
            validPsiMiFormats.add(FORMAT_PSI_MI);
            validPsiMiFormats.add(ProtocolConstants.FORMAT_HTML);
            validPsiMiFormats.add(FORMAT_COUNT_ONLY);
        }
        return validPsiMiFormats;
    }

    /**
     * Gets HashMap of Valid BioPax Formats.
     *
     * @return HashMap of Valid Formats.
     */
    public HashSet getValidBioPaxFormats() {
        if (validBioPaxFormats == null) {
            validBioPaxFormats = new HashSet();
            validBioPaxFormats.add(FORMAT_BIO_PAX);
            validBioPaxFormats.add(ProtocolConstants.FORMAT_HTML);
        }
        return validBioPaxFormats;
    }

}
