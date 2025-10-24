package care.smith.top.backend.service.datasource;

import static org.junit.jupiter.api.Assertions.*;

import care.smith.top.backend.model.jpa.datasource.EncounterDao;
import care.smith.top.backend.model.jpa.datasource.SubjectDao;
import care.smith.top.backend.model.jpa.datasource.SubjectResourceDao;
import care.smith.top.backend.util.AbstractJpaTest;
import java.util.List;

public class ImportTest extends AbstractJpaTest {
  void assertEncounters(EncounterDao... expected) {
    List<EncounterDao> actual = encounterRepository.findAll();
    assertEquals(expected.length, actual.size());
    assertTrue(actual.containsAll(List.of(expected)));
  }

  void assertSubjectResources(SubjectResourceDao... expected) {
    List<SubjectResourceDao> actual = subjectResourceRepository.findAll();
    assertEquals(expected.length, actual.size());
    assertTrue(actual.containsAll(List.of(expected)));
  }

  void assertSubjects(SubjectDao... expected) {
    List<SubjectDao> actual = subjectRepository.findAll();
    assertEquals(expected.length, actual.size());
    assertTrue(actual.containsAll(List.of(expected)));
  }
}
