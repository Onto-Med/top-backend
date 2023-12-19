package care.smith.top.backend.model.neo4j;

import care.smith.top.model.Document;
import java.util.Set;
import org.springframework.data.neo4j.core.schema.*;

@Node("Document")
public class DocumentNodeEntity {

  @Id
  @Property("docId")
  private final String documentId;

  @Property("name")
  private final String documentName;

  @Relationship(type = "HAS_PHRASE", direction = Relationship.Direction.OUTGOING)
  private Set<PhraseNodeEntity> documentPhrases;

  public DocumentNodeEntity(
      String documentId, String documentName, Set<PhraseNodeEntity> documentPhrases) {
    this.documentName = documentName;
    this.documentId = documentId;
    this.documentPhrases = documentPhrases;
  }

  public String documentId() {
    return documentId;
  }

  public String documentName() {
    return documentName;
  }

  public Set<PhraseNodeEntity> documentPhrases() {
    return documentPhrases;
  }

  public DocumentNodeEntity addPhrase(PhraseNodeEntity phraseNode) {
    this.documentPhrases.add(phraseNode);
    return this;
  }

  public DocumentNodeEntity removePhrase(PhraseNodeEntity phraseNode) {
    this.documentPhrases.remove(phraseNode);
    return this;
  }

  public Document toApiModel() {
    return new Document().id(documentId).name(documentName);
  }
}
