package care.smith.top.backend.model.jpa;

import care.smith.top.model.Code;
import care.smith.top.model.CodeSystem;
import java.net.URI;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

@Embeddable
public class CodeDao {
  @Column(nullable = false)
  private String code;

  private String name;

  private String uri;

  @Column(nullable = false)
  private String codeSystemUri;

  private String codeSystemVersion;

  public CodeDao() {}

  public CodeDao(@NotNull String code, String uri, @NotNull String codeSystemUri, String codeSystemVersion) {
    this.code = code;
    this.uri = uri;
    this.codeSystemUri = codeSystemUri;
    this.codeSystemVersion = codeSystemVersion;
  }

  public CodeDao(@NotNull String code, String name, String uri, @NotNull String codeSystemUri, String codeSystemVersion) {
    this.code = code;
    this.name = name;
    this.uri = uri;
    this.codeSystemUri = codeSystemUri;
    this.codeSystemVersion = codeSystemVersion;
  }

  public CodeDao(@NotNull Code code) {
    this.code = code.getCode();
    name = code.getName();
    if (code.getUri() != null) uri = code.getUri().toString();
    if (code.getCodeSystem() != null) {
      codeSystemUri = code.getCodeSystem().getUri().toString();
      codeSystemVersion = code.getCodeSystem().getVersion();
    }
  }

  public Code toApiModel() {
    return new Code()
        .code(code)
        .uri(uri != null ? URI.create(uri) : null)
        .name(name)
        .codeSystem(new CodeSystem()
                .uri(URI.create(codeSystemUri))
                .version(codeSystemVersion));
  }

  public String getCode() {
    return code;
  }

  public CodeDao code(@NotNull String code) {
    this.code = code;
    return this;
  }

  public String getName() {
    return name;
  }

  public CodeDao name(String name) {
    this.name = name;
    return this;
  }

  public String getUri() {
    return uri;
  }

  public CodeDao uri(String uri) {
    this.uri = uri;
    return this;
  }

  public String getCodeSystemUri() {
    return codeSystemUri;
  }

  public CodeDao codeSystemUri(@NotNull String codeSystemUri) {
    this.codeSystemUri = codeSystemUri;
    return this;
  }

  public String getCodeSystemVersion() {
    return codeSystemVersion;
  }

  public CodeDao codeSystemVersion(String codeSystemVersion) {
    this.codeSystemVersion = codeSystemVersion;
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
}
