package care.smith.top.backend.model.nlp;

import org.springframework.data.neo4j.core.schema.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Node("Concept")
public class ConceptEntity {

    @Id
    @GeneratedValue
    private Long id;

    @Property("conceptId")
    private final String conceptId;

    @Property("labels")
    private final List<String> labels;

    //    ToDo: this takes a long time to load; maybe it's not necessary bc I get the Phrases later with a Cypher query when needed
//    @Relationship(type = "IN_CONCEPT", direction = Relationship.Direction.INCOMING)
//    private Set<PhraseEntity> conceptPhrases;

    public ConceptEntity(String conceptId, List<String> labels){//, Set<PhraseEntity> conceptPhrases) {
        this.conceptId = conceptId;
        this.labels = labels;
//        this.conceptPhrases = conceptPhrases;
    }

    public ConceptEntity withId(Long id) {
        if (this.id.equals(id)) {
            return this;
        } else {
            ConceptEntity newObj = new ConceptEntity(this.conceptId, this.labels);//, this.conceptPhrases);
            newObj.id = id;
            return newObj;
        }
    }

    public String conceptId() {
        return this.conceptId;
    }

    public List<String> lables() {
        return this.labels;
    }

//    public Set<PhraseEntity> conceptPhrases() {
//        return this.conceptPhrases;
//    }
}
