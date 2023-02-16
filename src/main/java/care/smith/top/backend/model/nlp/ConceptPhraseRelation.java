package care.smith.top.backend.model.nlp;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@RelationshipProperties
public class ConceptPhraseRelation {

    @GeneratedValue
    @RelationshipId
    private Long id;

    @TargetNode
    private final ConceptEntity concept;

    public ConceptPhraseRelation(ConceptEntity concept) {
        this.concept = concept;
    }
}
