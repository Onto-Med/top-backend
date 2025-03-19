package care.smith.top.backend.model.neo4j;

import care.smith.top.model.Document;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.neo4j.core.schema.*;

@Node("Document")
public class DocumentNodeEntity {

  @Id
  @Property("docId")
  private final String documentId;

  @Property("name")
  private final String documentName;

  @Relationship(type = "HAS_PHRASE", direction = Relationship.Direction.OUTGOING)
  private Set<HasPhraseRelationship> documentPhrases;

  public DocumentNodeEntity(
      String documentId, String documentName, Set<PhraseNodeEntity> documentPhrases) {
    this.documentName = documentName;
    this.documentId = documentId;
    documentPhrases.forEach(phrase -> this.documentPhrases.add(new HasPhraseRelationship(phrase)));
  }

  public String documentId() {
    return documentId;
  }

  public String documentName() {
    return documentName;
  }

  public Set<PhraseNodeEntity> documentPhrases() {
    return documentPhrases.stream().map(HasPhraseRelationship::getPhrase).collect(Collectors.toSet());
  }

  public DocumentNodeEntity addPhrase(PhraseNodeEntity phraseNode) {
    this.documentPhrases.add(new HasPhraseRelationship(phraseNode));
    return this;
  }

  public DocumentNodeEntity removePhrase(PhraseNodeEntity phraseNode) {
    this.documentPhrases.stream().filter(r -> r.getPhrase().equals(phraseNode)).findFirst().ifPresent(r -> this.documentPhrases.remove(r));
    return this;
  }

  public Document toApiModel() {
    return new Document().id(documentId).name(documentName);
  }
}
