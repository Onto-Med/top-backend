package care.smith.top.backend.neo4j_ontology_access.model;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Version;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.time.Instant;

@Node
public class OntologyVersion {
  @Id @GeneratedValue private Long nodeId;
  @Version private Long nodeVersion;
  @CreatedBy private String createdBy;
  @CreatedDate private Instant createdAt;

  private int version;
  private Instant hiddenAt;

  public OntologyVersion() {}

  @PersistenceConstructor
  public OntologyVersion(Long nodeId) {
    this.nodeId = nodeId;
  }

  public Long getNodeId() {
    return nodeId;
  }

  public Long getNodeVersion() {
    return nodeVersion;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public int getVersion() {
    return version;
  }

  public OntologyVersion setVersion(int version) {
    this.version = version;
    return this;
  }

  public Instant getHiddenAt() {
    return hiddenAt;
  }

  public OntologyVersion setHiddenAt(Instant hiddenAt) {
    this.hiddenAt = hiddenAt;
    return this;
  }
}
