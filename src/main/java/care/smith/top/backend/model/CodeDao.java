package care.smith.top.backend.model;

import care.smith.top.model.Code;
import care.smith.top.model.CodeSystem;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import java.net.URI;

@Embeddable
public class CodeDao {
  @Column(nullable = false)
  private String code;

  private String name;

  @Column(nullable = false)
  private URI codeSystemUri;

  private String codeSystemName;

  public CodeDao() {}

  public CodeDao(String code, URI codeSystemUri) {
    this.code = code;
    this.codeSystemUri = codeSystemUri;
  }

  public CodeDao(String code, String name, URI codeSystemUri, String codeSystemName) {
    this.code = code;
    this.name = name;
    this.codeSystemUri = codeSystemUri;
    this.codeSystemName = codeSystemName;
  }

  public CodeDao(@NotNull Code code) {
    this.code = code.getCode();
    name = code.getName();
    if (code.getCodeSystem() != null) {
      codeSystemUri = code.getCodeSystem().getUri();
      codeSystemName = code.getCodeSystem().getName();
    }
  }

  public Code toApiModel() {
    return new Code()
        .code(code)
        .name(name)
        .codeSystem(new CodeSystem().uri(codeSystemUri).name(codeSystemName));
  }

  public String getCode() {
    return code;
  }

  public CodeDao code(String code) {
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

  public URI getCodeSystemUri() {
    return codeSystemUri;
  }

  public CodeDao codeSystemUri(URI codeSystemUri) {
    this.codeSystemUri = codeSystemUri;
    return this;
  }

  public String getCodeSystemName() {
    return codeSystemName;
  }

  public CodeDao codeSystemName(String codeSystemName) {
    this.codeSystemName = codeSystemName;
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
    if (!getCodeSystemUri().equals(codeDao.getCodeSystemUri())) return false;
    return getCodeSystemName() != null
        ? getCodeSystemName().equals(codeDao.getCodeSystemName())
        : codeDao.getCodeSystemName() == null;
  }

  @Override
  public int hashCode() {
    int result = getCode().hashCode();
    result = 31 * result + (getName() != null ? getName().hashCode() : 0);
    result = 31 * result + getCodeSystemUri().hashCode();
    result = 31 * result + (getCodeSystemName() != null ? getCodeSystemName().hashCode() : 0);
    return result;
  }
}
