package care.smith.top.backend.model.nlp;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;
import java.util.Map;

@Document(indexName = "#{ @elasticsearchConfigBean.getIndexName() }", createIndex = false)
public class DocumentEntity {
    @Id
    private String id;

    @Field(name = "name", type = FieldType.Keyword)
    private String documentName;

    @Field(name = "text", type = FieldType.Text)
    private String documentText;

    private Map<String, List<String>> highlights;

    public String getId() { return id; };

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentText(String text) {
        this.documentText = text;
    }

    public String getDocumentText() {
        return documentText;
    }

    public void setHighlights(Map<String, List<String>> highlights) {
        this.highlights = highlights;
    }

    public Map<String, List<String>> getHighlights() {
        if (this.highlights == null) {
            return Map.of("documentText", List.of(getDocumentText()));
        }
        return highlights;
    }
}
