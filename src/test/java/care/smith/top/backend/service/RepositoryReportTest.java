package care.smith.top.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import care.smith.top.backend.model.jpa.*;
import care.smith.top.backend.model.jpa.datasource.*;
import care.smith.top.backend.repository.jpa.*;
import care.smith.top.backend.repository.jpa.datasource.*;
import care.smith.top.model.*;
import care.smith.top.top_phenotypic_query.result.ResultSet;
import java.lang.reflect.Field;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

class RepositoryReportTest {
  private final RepositoryService service = new RepositoryService();
  private final EncounterRepository encounters = mock(EncounterRepository.class);
  private final ExpectedResultRepository expectations = mock(ExpectedResultRepository.class);
  private final PhenotypeQueryService queries = mock(PhenotypeQueryService.class);
  private final ResultSet results = new ResultSet();

  private void inject(String name, Object value) throws Exception {
    Field field = RepositoryService.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(service, value);
  }

  private void prepare(int count, boolean contactsExist) throws Exception {
    RepositoryRepository repositories = mock(RepositoryRepository.class);
    PhenotypeRepository phenotypes = mock(PhenotypeRepository.class);
    OrganisationDao organisation = mock(OrganisationDao.class);
    when(organisation.hasDataSource("source")).thenReturn(true);
    RepositoryDao repository =
        new RepositoryDao()
            .organisation(organisation)
            .repositoryType(RepositoryType.PHENOTYPE_REPOSITORY);
    when(repositories.findByIdAndOrganisationId("repo", "org")).thenReturn(Optional.of(repository));
    EntityDao first = mock(EntityDao.class);
    EntityDao second = mock(EntityDao.class);
    when(first.getId()).thenReturn("p1");
    when(second.getId()).thenReturn("p2");
    when(phenotypes.findAllByRepositoryId(eq("repo"), any()))
        .thenReturn(new PageImpl<>(List.of(first, second)));
    List<ExpectedResultDao> expected = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      String encounterId = "e" + i;
      for (String phenotype : List.of("p1", "p2")) {
        results.addValue(
            encounterId, phenotype, null, new BooleanValue(DataType.BOOLEAN).value(true));
        expected.add(
            new ExpectedResultDao("source", encounterId + phenotype)
                .encounterId(encounterId)
                .phenotypeId(phenotype)
                .booleanValue(true));
      }
    }
    when(expectations.findAllByExpectedResultKeyDataSourceId("source")).thenReturn(expected);
    when(queries.executeQuery(any(PhenotypeQuery.class), eq("repo"))).thenReturn(results);
    when(encounters.findAllByEncounterKeyDataSourceIdAndEncounterKeyEncounterIdIn(
            eq("source"), anyCollection()))
        .thenAnswer(
            call -> {
              Collection<String> ids = call.getArgument(1);
              return contactsExist
                  ? ids.stream()
                      .map(id -> new EncounterDao("source", id).subjectId("s" + id))
                      .toList()
                  : List.of();
            });
    inject("repositoryRepository", repositories);
    inject("phenotypeRepository", phenotypes);
    inject("expectedResultRepository", expectations);
    inject("encounterRepository", encounters);
    inject("phenotypeQueryService", queries);
  }

  @Test
  void sharesContactsAcrossPhenotypesAndPreservesReports() throws Exception {
    prepare(2, true);
    // Extra values exercise contact-to-subject mapping in additional report rows.
    results.addValue("e0", "p1", null, new BooleanValue(DataType.BOOLEAN).value(false));
    List<TestReport> reports = service.testRepository("org", "repo", "source");
    assertEquals(5, reports.size());
    assertEquals(4, reports.stream().filter(r -> Boolean.TRUE.equals(r.isPassed())).count());
    TestReport extra = reports.stream().filter(r -> r.isPassed() == null).findFirst().orElseThrow();
    assertEquals("se0", extra.getSubjectId());
    assertEquals("e0", extra.getEncounterId());
    verify(encounters)
        .findAllByEncounterKeyDataSourceIdAndEncounterKeyEncounterIdIn(
            "source", List.of("e0", "e1"));
    verifyNoMoreInteractions(encounters);
  }

  @Test
  void boundsQueriesForLargeReports() throws Exception {
    prepare(1001, true);
    assertEquals(2002, service.testRepository("org", "repo", "source").size());
    verify(encounters, times(2))
        .findAllByEncounterKeyDataSourceIdAndEncounterKeyEncounterIdIn(
            eq("source"), argThat(ids -> ids.size() <= 1000));
    verifyNoMoreInteractions(encounters);
  }

  @Test
  void doesNotQueryContactsForEmptyResults() throws Exception {
    prepare(1, true);
    results.clear();
    assertEquals(2, service.testRepository("org", "repo", "source").size());
    verifyNoInteractions(encounters);
  }

  @Test
  void leavesMissingContactsUnassigned() throws Exception {
    prepare(1, false);
    // A single projected result avoids changing the existing null-subject sort behavior.
    results.getPhenotypes("e0").remove("p2");
    List<TestReport> reports = service.testRepository("org", "repo", "source");
    assertEquals(2, reports.size());
    assertTrue(reports.stream().allMatch(r -> r.getSubjectId() == null));
  }
}
