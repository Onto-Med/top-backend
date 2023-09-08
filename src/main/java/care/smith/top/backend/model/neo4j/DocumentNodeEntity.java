package care.smith.top.backend.model.neo4j;

import java.util.List;

import care.smith.top.model.Document;
import org.springframework.data.neo4j.core.schema.*;

@Node("Document")
public class DocumentNodeEntity {

  @Property("docId")
  private final String documentId;

  @Property("name")
  private final String documentName;

  @Id @GeneratedValue private Long id;
  // @Relationship(type = "HAS_PHRASE", direction = Relationship.Direction.OUTGOING)
  private List<PhraseNodeEntity> documentPhrases;

  public DocumentNodeEntity(String documentId, String documentName) {
    this.id = null;
    this.documentName = documentName;
    this.documentId = documentId;
  }

  public DocumentNodeEntity withId(Long id) {
    if (this.id.equals(id)) {
      return this;
    } else {
      DocumentNodeEntity newObj = new DocumentNodeEntity(this.documentId, this.documentName);
      newObj.id = id;
      return newObj;
    }
  }

  public String documentId() {
    return documentId;
  }

  public String documentName() {
    return documentName;
  }

  public List<PhraseNodeEntity> documentPhrases() {
    return documentPhrases;
  }

  public Document toApiModel() {
    return new Document()
        .id(documentId)
        .name(documentName);
  }
}
