package care.smith.top.backend.model.nlp;

import org.springframework.data.neo4j.core.schema.*;

import java.util.List;
import java.util.stream.Collectors;

@Node("Document")
public class DocumentEntity {

    @Id @GeneratedValue private Long id;

    @Property("docId")
    private final String documentId;

    @Property("text")
    private final String documentText;

    @Relationship(type = "HAS_PHRASE", direction = Relationship.Direction.OUTGOING)
    private List<PhraseEntity> documentPhrases;

    public DocumentEntity(String documentId, String documentText) {
        this.id = null;
        this.documentId = documentId;
        this.documentText = documentText;
    }

    public DocumentEntity withId(Long id) {
        if (this.id.equals(id)) {
            return this;
        } else {
            DocumentEntity newObj = new DocumentEntity(this.documentId, this.documentText);
            newObj.id = id;
            return newObj;
        }
    }

    public String documentId() {
        return documentId;
    }

    public String documentText() {
        return documentText;
    }

    public List<PhraseEntity> documentPhrases() { return documentPhrases; }
}
