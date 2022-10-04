package care.smith.top.backend.model.nlp;

import org.springframework.data.neo4j.core.schema.*;

import java.util.List;

@Node("Phrase")
public class PhraseEntity {

    @Id
    @GeneratedValue
    private Long id;

    @Property("attributes")
    private final List<String> phraseAttributes;

    @Property("exemplar")
    private final Boolean isExemplar;

    @Property("phrase")
    private final String phraseText;

    @Property("phraseId")
    private final String phraseId;

    @Relationship(type = "PARENT_OF")
    private List<PhraseEntity> phrases;

    public PhraseEntity(List<String> phraseAttributes, Boolean isExemplar, String phraseText, String phraseId) {
        this.id = null;
        this.phraseAttributes = phraseAttributes;
        this.isExemplar = isExemplar;
        this.phraseText = phraseText;
        this.phraseId = phraseId;
    }

    public PhraseEntity withId(Long id) {
        if (this.id.equals(id)) {
            return this;
        } else {
            PhraseEntity newObj = new PhraseEntity(this.phraseAttributes, this.isExemplar, this.phraseText, this.phraseId);
            newObj.id = id;
            return newObj;
        }
    }

    public List<String> phraseAttributes() { return this.phraseAttributes; }

    public String phraseText() { return this.phraseText; }

    public Boolean isExemplar() { return this.isExemplar; }

    public String phraseId() { return this.phraseId; }
}
