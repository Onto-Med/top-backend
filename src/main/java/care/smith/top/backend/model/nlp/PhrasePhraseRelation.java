package care.smith.top.backend.model.nlp;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@RelationshipProperties
public class PhrasePhraseRelation {

    @GeneratedValue
    @RelationshipId
    private Long id;

    private final Float significance;

    private final Float weight;

    private final Boolean inSubcluster;

    @TargetNode
    private final PhraseEntity phrase;

    public PhrasePhraseRelation(Float significance, Float weight, Boolean inSubcluster, PhraseEntity phrase) {
        this.significance = significance;
        this.weight = weight;
        this.inSubcluster = inSubcluster;
        this.phrase = phrase;
    }

    public Float significance() { return this.significance; }

    public Float weight() { return this.weight; }

    public Boolean inSubcluster() { return this.inSubcluster; }
}
