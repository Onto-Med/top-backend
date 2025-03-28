package care.smith.top.backend.util.nlp;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public class DocumentOffset implements Comparable<DocumentOffset> {
  private Integer begin;
  private Integer end;

  public static DocumentOffset of(String offsetString) {
    return DocumentOffset.of(offsetString, ",");
  }

  public static DocumentOffset of(String offsetString, String delimiter) {
    String[] parts = offsetString.split(delimiter);
    if (parts.length < 2) return null;
    return new DocumentOffset(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
  }

  public static DocumentOffset of(List<Integer> offset) {
    if (offset.size() >= 2) return new DocumentOffset(offset.get(0), offset.get(1));
    return null;
  }

  @JsonCreator
  public DocumentOffset(@JsonProperty("begin") Integer begin, @JsonProperty("end") Integer end) {
    this.begin = begin;
    this.end = end;
  }

  public Integer getBegin() {
    return begin;
  }

  public void setBegin(Integer begin) {
    this.begin = begin;
  }

  public Integer getEnd() {
    return end;
  }

  public void setEnd(Integer end) {
    this.end = end;
  }

  public boolean overlaps(@NotNull DocumentOffset o) {
    if (this.begin != null && this.end != null && o.begin != 0 && o.end != null) {
      return (this.begin > o.begin && o.end > this.begin)
          || (this.end > o.begin && o.end > this.end)
          || (this.begin < o.begin && this.end > o.end)
          || (this.begin > o.begin && this.end < o.end);
    }
    return false;
  }

  public boolean contains(@NotNull DocumentOffset o) {
    if (this.begin != null && this.end != null && o.begin != 0 && o.end != null) {
      return (this.begin < o.begin && this.end > o.end);
    }
    return false;
  }

  @Override
  public int compareTo(@NotNull DocumentOffset o) {
    if (Objects.equals(this.getBegin(), o.getBegin())) {
      return this.getEnd().compareTo(o.getEnd());
    } else if (this.getBegin() < o.getBegin()) {
      return -1;
    } else {
      return 1;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    DocumentOffset that = (DocumentOffset) o;
    return Objects.equals(begin, that.begin) && Objects.equals(end, that.end);
  }

  @Override
  public int hashCode() {
    return Objects.hash(begin, end);
  }
}
