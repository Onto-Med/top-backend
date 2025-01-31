package care.smith.top.backend.service.datasource;

import static org.junit.jupiter.api.Assertions.assertEquals;

import care.smith.top.backend.AbstractTest;
import care.smith.top.backend.model.jpa.datasource.EncounterDao;
import care.smith.top.backend.model.jpa.datasource.SubjectDao;
import care.smith.top.backend.model.jpa.datasource.SubjectResourceDao;
import java.util.List;
import java.util.stream.IntStream;

public class ImportTest extends AbstractTest {
  void assertEncounters(EncounterDao... expected) {
    List<EncounterDao> actual = encounterRepository.findAll();
    assertEquals(expected.length, actual.size());
    IntStream.range(0, expected.length).forEach(i -> assertEquals(expected[i], actual.get(i)));
  }

  void assertSubjectResources(SubjectResourceDao... expected) {
    List<SubjectResourceDao> actual = subjectResourceRepository.findAll();
    assertEquals(expected.length, actual.size());
    IntStream.range(0, expected.length).forEach(i -> assertEquals(expected[i], actual.get(i)));
  }

  void assertSubjects(SubjectDao... expected) {
    List<SubjectDao> actual = subjectRepository.findAll();
    assertEquals(expected.length, actual.size());
    IntStream.range(0, expected.length).forEach(i -> assertEquals(expected[i], actual.get(i)));
  }
}
