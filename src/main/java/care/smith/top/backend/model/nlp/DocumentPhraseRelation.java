package care.smith.top.backend.model.nlp;

import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@RelationshipProperties
public class DocumentPhraseRelation {

    @RelationshipId
    private Long id;

    @TargetNode
    private final PhraseEntity phrase;

    public DocumentPhraseRelation(PhraseEntity phrase) {
        this.phrase = phrase;
    }

    public PhraseEntity phrase() { return this.phrase; }
}
