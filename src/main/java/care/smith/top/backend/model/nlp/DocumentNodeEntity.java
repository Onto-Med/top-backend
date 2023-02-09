package care.smith.top.backend.model.nlp;

import org.springframework.data.neo4j.core.schema.*;

import java.util.List;

@Node("Document")
public class DocumentNodeEntity {

    @Id @GeneratedValue private Long id;

    @Property("docId")
    private final String documentId;

    @Relationship(type = "HAS_PHRASE", direction = Relationship.Direction.OUTGOING)
    private List<PhraseEntity> documentPhrases;

    public DocumentNodeEntity(String documentId) {
        this.id = null;
        this.documentId = documentId;
    }

    public DocumentNodeEntity withId(Long id) {
        if (this.id.equals(id)) {
            return this;
        } else {
            DocumentNodeEntity newObj = new DocumentNodeEntity(this.documentId);
            newObj.id = id;
            return newObj;
        }
    }

    public String documentId() {
        return documentId;
    }

    public List<PhraseEntity> documentPhrases() { return documentPhrases; }
}
