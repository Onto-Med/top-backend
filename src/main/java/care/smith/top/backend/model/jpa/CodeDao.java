package care.smith.top.backend.model.jpa;

import care.smith.top.model.Code;
import care.smith.top.model.CodeSystem;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Entity(name = "code")
public class CodeDao {

  @Id @GeneratedValue private Long id;

  @ManyToOne()
  @JoinColumn(name = "parent_id", referencedColumnName = "id")
  private CodeDao parent;

  @OrderColumn
  @OneToMany(
      cascade = {CascadeType.ALL},
      orphanRemoval = true)
  @JoinColumn(name = "parent_id")
  private List<CodeDao> children;

  @Column(nullable = false)
  private String code;

  private String name;

  private String uri;

  @Column(nullable = false)
  private String codeSystemUri;

  public CodeDao() {}

  public CodeDao(@NotNull String code, String uri, @NotNull String codeSystemUri) {
    this.code = code;
    this.uri = uri;
    this.codeSystemUri = codeSystemUri;
  }

  public CodeDao(@NotNull String code, String name, String uri, @NotNull String codeSystemUri) {
    this.code = code;
    this.name = name;
    this.uri = uri;
    this.codeSystemUri = codeSystemUri;
  }

  public CodeDao(@NotNull Code code) {
    this.code = code.getCode();
    name = code.getName();
    if (code.getUri() != null) uri = code.getUri().toString();
    if (code.getCodeSystem() != null) codeSystemUri = code.getCodeSystem().getUri().toString();
    this.children =
        Optional.ofNullable(code.getChildren()).orElse(Collections.emptyList()).stream()
            .map(c -> new CodeDao(c).parent(this))
            .collect(Collectors.toList());
  }

  public Code toApiModel() {
    return new Code(new CodeSystem(URI.create(codeSystemUri)), code)
        .uri(uri != null ? URI.create(uri) : null)
        .name(name)
        .synonyms(Collections.emptyList())
        .children(new ArrayList<>());
  }

  public CodeDao id(@NotNull Long id) {
    this.id = id;
    return this;
  }

  public CodeDao parent(@NotNull CodeDao parent) {
    this.parent = parent;
    return this;
  }

  public CodeDao children(List<CodeDao> children) {
    this.children = children;
    return this;
  }

  public CodeDao code(@NotNull String code) {
    this.code = code;
    return this;
  }

  public CodeDao name(String name) {
    this.name = name;
    return this;
  }

  public CodeDao uri(String uri) {
    this.uri = uri;
    return this;
  }

  public CodeDao codeSystemUri(@NotNull String codeSystemUri) {
    this.codeSystemUri = codeSystemUri;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CodeDao codeDao = (CodeDao) o;

    if (!getCode().equals(codeDao.getCode())) return false;
    if (getName() != null ? !getName().equals(codeDao.getName()) : codeDao.getName() != null)
      return false;
    if (getUri() != null ? !getUri().equals(codeDao.getUri()) : codeDao.getUri() != null)
      return false;
    return getCodeSystemUri().equals(codeDao.getCodeSystemUri());
  }

  @Override
  public int hashCode() {
    int result = getCode().hashCode();
    result = 31 * result + (getName() != null ? getName().hashCode() : 0);
    result = 31 * result + (getUri() != null ? getUri().hashCode() : 0);
    result = 31 * result + getCodeSystemUri().hashCode();
    return result;
  }

  public Long getId() {
    return id;
  }

  public CodeDao getParent() {
    return parent;
  }

  public List<CodeDao> getChildren() {
    return children;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getUri() {
    return uri;
  }

  public String getCodeSystemUri() {
    return codeSystemUri;
  }
}
