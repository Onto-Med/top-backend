package care.smith.top.backend.model.nlp;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "#{ @elasticsearchConfigBean.getIndexName() }", createIndex = false)
public class DocumentEntity {
    @Id
    private String id;

    @Field(name = "name", type = FieldType.Keyword)
    private String documentName;

    @Field(name = "text", type = FieldType.Text)
    private String documentText;

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
}
