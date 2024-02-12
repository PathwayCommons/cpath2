package cpath.service.api;

/**
 * Values to generate standard BioPAX RelationshipTypeVocabulary objects.
 */
public enum RelTypeVocab {
    IDENTITY("identity", "bioregistry.io/mi:0356", "mi", "0356"),
    SECONDARY_ACCESSION_NUMBER("secondary-ac", "bioregistry.io/mi:0360", "mi", "0360"),
    ADDITIONAL_INFORMATION("see-also", "bioregistry.io/mi:0361", "mi", "0361"),
    //next should work for rel. xrefs pointing to a protein but attached to a Gene, Dna*, Rna* objects
    GENE_PRODUCT("gene product", "bioregistry.io/mi:0251", "mi", "0251"),
    SET_MEMBER("set member", "bioregistry.io/mi:1341", "mi", "1341"),
    //next one is for chebi "is_a" relationships (when parent is a chemical class/concept rather than compound)
    MULTIPLE_PARENT_REFERENCE("multiple parent reference", "bioregistry.io/mi:0829", "mi", "0829"),
    ISOFORM_PARENT("isoform-parent", "bioregistry.io/mi:0243", "mi", "0243"),;

    public final String term;
    public final String uri;
    public final String db;
    public final String id;

    RelTypeVocab(String term, String uri, String db, String id) {
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