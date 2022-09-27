package care.smith.top.backend.model;

import care.smith.top.model.LocalisableText;

import javax.persistence.Embeddable;

@Embeddable
public class LocalisableTextDao {
  private String lang;
  private String text;

  public LocalisableTextDao() {}

  public LocalisableTextDao(String lang, String text) {
    this.lang = lang;
    this.text = text;
  }

  public LocalisableTextDao lang(String lang) {
    this.lang = lang;
    return this;
  }

  public LocalisableTextDao text(String text) {
    this.text = text;
    return this;
  }

  public LocalisableTextDao(LocalisableText localisableText) {
    lang = localisableText.getLang();
    text = localisableText.getText();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    LocalisableTextDao that = (LocalisableTextDao) o;

    if (getLang() != null ? !getLang().equals(that.getLang()) : that.getLang() != null)
      return false;
    return getText() != null ? getText().equals(that.getText()) : that.getText() == null;
  }

  @Override
  public int hashCode() {
    int result = getLang() != null ? getLang().hashCode() : 0;
    result = 31 * result + (getText() != null ? getText().hashCode() : 0);
    return result;
  }

  public String getLang() {
    return lang;
  }

  public String getText() {
    return text;
  }
}
