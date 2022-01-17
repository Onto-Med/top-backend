package care.smith.top.backend.neo4j_ontology_access;

import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Node
public class Class extends care.smith.top.simple_onto_api.model.ClassDef {

  @CompositeProperty(prefix = "title")
  Map<String, Object> titles = new HashMap<>();

  @Id @GeneratedValue private Long id;

  public Class() {
    super(UUID.randomUUID().toString());
  }

  public Class(String name) {
    super(name);
  }

  public Class addTitle(String lang, String title) {
    this.titles.put(lang, title);
    return this;
  }
}
