package care.smith.top.backend.model;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("Phrase")
public class PhraseEntity {

    @Id @GeneratedValue private Long id;
}
