package care.smith.top.backend.model.elasticsearch;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "#{ @elasticsearchConfigBean.getIndexName() }", createIndex = false)
public class DocumentEntity {

  @Id private String id;

  // ToDo: @Value does not work; find a way to change this via config
  @Value("${top.documents.ellipsis:15}")
  private Integer wordEllipsis = 25;

  @Field(name = "name", type = FieldType.Keyword)
  private String documentName;

  @Field(name = "text", type = FieldType.Text)
  private String documentText;

  @Field(name = "id", type = FieldType.Keyword)
  private String documentId;

  private Map<String, List<String>> highlights;

  public care.smith.top.model.Document toApiModel() {
    return new care.smith.top.model.Document()
        .id(id)
        .name(documentName)
        .text(
            Arrays.stream(documentText.split("\\s+"))
                .limit(wordEllipsis)
                .collect(Collectors.joining(" ")))
        .highlightedText(
            this.getHighlights().values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.joining()));
  }
  ;

  public care.smith.top.model.Document nullDocument() {
    return new care.smith.top.model.Document()
        .id("null")
        .name("null")
        .text("null")
        .highlightedText("null");
  }

  public String getId() {
    return id;
  }

  public String getDocumentName() {
    return documentName;
  }

  public void setDocumentName(String documentName) {
    this.documentName = documentName;
  }

  public String getDocumentText() {
    return documentText;
  }

  public void setDocumentText(String text) {
    this.documentText = text;
  }

  public Map<String, List<String>> getHighlights() {
    if (this.highlights == null) {
      return Map.of("documentText", List.of(getDocumentText()));
    }
    return highlights;
  }

  public void setHighlights(Map<String, List<String>> highlights) {
    this.highlights = highlights;
  }
}
