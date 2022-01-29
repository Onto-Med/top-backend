package care.smith.top.backend.neo4j_ontology_access.model;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Version;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.security.core.userdetails.User;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Node({ "OntologyVersion", "RelationOwner" })
public class OntologyVersion implements ClassRelationOwner {
  @Id @GeneratedValue private Long nodeId;
  @Version private Long nodeVersion;
  @CreatedBy private User createdBy;
  @CreatedDate private Instant createdAt;

  @Relationship(type = "HAS_ROOT_CLASS")
  private Set<RootClass> rootClasses = null;

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

  public User getCreatedBy() {
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

  @Override
  public ClassRelationOwner addRootClass(RootClass rootClass) {
    if (rootClasses == null) rootClasses = new HashSet<>();
    rootClasses.add(rootClass);
    return this;
  }

  @Override
  public Set<RootClass> getRootClasses() {
    return rootClasses;
  }

  public OntologyVersion setRootClasses(Set<RootClass> rootClasses) {
    this.rootClasses = rootClasses;
    return this;
  }
}
