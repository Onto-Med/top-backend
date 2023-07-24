package care.smith.top.backend.model.elasticsearch;

import java.util.List;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "#{ @elasticsearchConfigBean.getIndexName() }", createIndex = false)
public class DocumentEntity {
  @Id private String id;

  @Field(name = "name", type = FieldType.Keyword)
  private String documentName;

  @Field(name = "text", type = FieldType.Text)
  private String documentText;

  private Map<String, List<String>> highlights;

  public String getId() {
    return id;
  }
  ;

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
