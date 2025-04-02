package care.smith.top.backend.util.nlp;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class DocumentOffsetTest {

  DocumentOffset offset1 = DocumentOffset.of("0-4", "-");
  DocumentOffset offset1_std_delim = DocumentOffset.of("0,4");
  DocumentOffset offset1_int = DocumentOffset.of(List.of(0, 4));
  DocumentOffset offset1_constructor = new DocumentOffset(0, 4);
  DocumentOffset offset2 = DocumentOffset.of("4-12", "-");
  DocumentOffset offset3 = DocumentOffset.of("7-10", "-");
  DocumentOffset offset4 = DocumentOffset.of("9-15", "-");
  DocumentOffset offset5 = DocumentOffset.of("4-10", "-");

  @Test
  void equals() {
    assertEquals(offset1, offset1_int);
    assertEquals(offset1, offset1_constructor);
    assertEquals(offset1, offset1_std_delim);
  }

  @Test
  void overlaps() {
    assertFalse(offset1.overlaps(offset2));
    assertTrue(offset2.overlaps(offset3));
    assertTrue(offset3.overlaps(offset2));
    assertTrue(offset3.overlaps(offset4));
    assertTrue(offset4.overlaps(offset3));
  }

  @Test
  void contains() {
    assertTrue(offset2.contains(offset3));
    assertTrue(offset2.contains(offset5));
    assertTrue(offset1.contains(offset1_int));
    assertFalse(offset3.contains(offset2));
    assertFalse(offset3.contains(offset4));
  }

  @Test
  void compareTo() {
    assertEquals(0, offset1.compareTo(offset1_int));
    assertTrue(offset1.compareTo(offset1_int) == 0);
    assertTrue(offset1.compareTo(offset2) < 0);
    assertTrue(offset2.compareTo(offset3) > 0);
    assertTrue(offset2.compareTo(offset4) < 0);
    assertTrue(offset4.compareTo(offset3) > 0);
  }

  @Test
  void faultyOffset() {
    assertNull(DocumentOffset.of("4", "-"));
    assertNull(DocumentOffset.of(List.of(0)));
  }
}
