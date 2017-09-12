package cpath.service;

/**
 * Values to generate standard BioPAX RelationshipTypeVocabulary objects.
 */
public enum RelTypeVocab {
    IDENTITY("identity", "http://identifiers.org/psimi/MI:0356", "MI", "MI:0356"),
    SECONDARY_ACCESSION_NUMBER("secondary-ac", "http://identifiers.org/psimi/MI:0360", "MI", "MI:0360"),
    ADDITIONAL_INFORMATION("see-also", "http://identifiers.org/psimi/MI:0361", "MI", "MI:0361"),
    //next should work for rel. xrefs pointing to a protein but attached to a Gene, Dna*, Rna* biopax objects
    GENE_PRODUCT("gene product", "http://identifiers.org/psimi/MI:0251", "MI", "MI:0251"),
    SET_MEMBER("set member", "http://identifiers.org/psimi/MI:1341", "MI", "MI:1341"),
    //next one is probably for chebi "is_a" relationships (when parent is a chemical class/concept rather than compound)
    MULTIPLE_PARENT_REFERENCE("multiple parent reference", "http://identifiers.org/psimi/MI:0829", "MI", "MI:0829"),
    ISOFORM_PARENT("isoform-parent", "http://identifiers.org/psimi/MI:0243", "MI", "MI:0243"),;

    public final String term;
    public final String uri;
    public final String db;
    public final String id;

    private RelTypeVocab(String term, String uri, String db, String id) {
        this.term = term;
        this.uri = uri;
        this.db = db;
        this.id = id;
    }

    @Override
    public String toString() {
        return term;
    }
}