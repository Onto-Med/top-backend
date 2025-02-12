package care.smith.top.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import care.smith.top.backend.AbstractTest;
import care.smith.top.backend.repository.ols.CodeRepository;
import care.smith.top.backend.repository.ols.CodeSystemRepository;
import care.smith.top.model.*;
import java.net.URI;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class CodeServiceTest extends AbstractTest {

  @Autowired private CodeSystemRepository codeSystemRepository;
  @Autowired private CodeRepository codeRepository;

  private static final class UriCodeScopeChildCountTuple {
    URI uri;
    Integer subtree;
    Integer leaves;

    UriCodeScopeChildCountTuple(URI uri, Integer subtree, Integer leaves) {
      this.uri = uri;
      this.subtree = subtree;
      this.leaves = leaves;
    }

    static UriCodeScopeChildCountTuple of(URI uri, Integer subtree, Integer leaves) {
      return new UriCodeScopeChildCountTuple(uri, subtree, leaves);
    }

    Integer value(CodeScope scope) {
      switch (scope) {
        case SELF:
          return 0;
        case SUBTREE:
          return subtree;
        case LEAVES:
          return leaves;
        default:
          throw new AssertionError(scope.toString());
      }
    }
  }

  private static final Stream<Arguments> provideTestValuesForSubtrees() {
    return Stream.of(
        Arguments.of("test-1", 2, 1),
        Arguments.of("test-11", 1, 1),
        Arguments.of("test-2", 8, 5),
        Arguments.of("test-21", 6, 4),
        Arguments.of("test-211", 1, 1),
        Arguments.of("test-212", 4, 3),
        Arguments.of("test-2121", 1, 1),
        Arguments.of("test-2122", 1, 1),
        Arguments.of("test-2123", 1, 1),
        Arguments.of("test-22", 1, 1),
        Arguments.of("test-3", 7, 5),
        Arguments.of("test-31", 4, 3),
        Arguments.of("test-311", 1, 1),
        Arguments.of("test-312", 1, 1),
        Arguments.of("test-313", 1, 1),
        Arguments.of("test-32", 1, 1),
        Arguments.of("test-33", 1, 1),
        Arguments.of("test-4", 5, 4),
        Arguments.of("test-41", 1, 1),
        Arguments.of("test-42", 1, 1),
        Arguments.of("test-43", 1, 1),
        Arguments.of("test-44", 1, 1),
        Arguments.of("test-5", 6, 5),
        Arguments.of("test-51", 1, 1),
        Arguments.of("test-52", 1, 1),
        Arguments.of("test-53", 1, 1),
        Arguments.of("test-54", 1, 1),
        Arguments.of("test-55", 1, 1));
  }

  @Autowired OLSCodeService codeService;

  @Test
  void getSuggestions() {
    CodePage suggestions = codeService.getCodeSuggestions(null, "term", Collections.emptyList(), 1);
    assertThat(suggestions).isNotNull().satisfies(s -> assertThat(s.getContent()).isNotEmpty());
  }

  @Test
  void getCodeSystems() {
    CodeSystemPage codeSystems = codeService.getCodeSystems(null, null, null, 1);
    assertThat(codeSystems).isNotNull().satisfies(cs -> assertThat(cs.getContent()).isNotEmpty());
  }

  @ParameterizedTest
  @MethodSource("provideTestValuesForSubtrees")
  void createCodesWithSubtrees(String codeName, int expectedSubtreeSize, int expectedLeafCount) {
    Organisation organisation =
        organisationService.createOrganisation(new Organisation().id("org"));
    Repository repository =
        repositoryService.createRepository(
            organisation.getId(),
            new Repository().id("repo").repositoryType(RepositoryType.PHENOTYPE_REPOSITORY),
            null);

    Arrays.stream(CodeScope.values())
        .forEach(
            scope -> {
              Category category = new Category();
              Code code =
                  codeService.getCode(
                      URI.create("http://top.smith.care/test/terminology#" + codeName),
                      "test",
                      scope);

              category
                  .id(UUID.randomUUID().toString())
                  .entityType(EntityType.CATEGORY)
                  .addCodesItem(code);

              assertThat(
                      entityService.createEntity(
                          organisation.getId(), repository.getId(), category))
                  .isNotNull()
                  .isInstanceOf(Category.class)
                  .satisfies(
                      c -> {
                        assertThat(c.getCodes()).size().isEqualTo(1);
                        Code codeEntity = c.getCodes().get(0);
                        fillInCodeSystems(codeEntity);

                        assertThat(codeEntity).isEqualTo(code);

                        switch (scope) {
                          case SUBTREE:
                            assertThat(nodeCount(code)).isEqualTo(expectedSubtreeSize);
                            assertThat(nodeCount(codeEntity)).isEqualTo(expectedSubtreeSize);
                            break;
                          case LEAVES:
                            assertThat(leafCount(code)).isEqualTo(expectedLeafCount);
                            assertThat(leafCount(codeEntity)).isEqualTo(expectedLeafCount);
                            break;
                          case SELF:
                            assertThat(leafCount(code)).isEqualTo(1);
                            assertThat(leafCount(codeEntity)).isEqualTo(1);
                            break;
                        }
                      });
            });
  }

  private int nodeCount(Code c) {
    return 1 + c.getChildren().stream().map(child -> nodeCount(child)).reduce(0, Integer::sum);
  }

  private int leafCount(Code c) {
    return isLeaf(c)
        ? 1
        : c.getChildren().stream().map(child -> leafCount(child)).reduce(0, Integer::sum);
  }

  private boolean isLeaf(Code c) {
    return c.getChildren() == null || c.getChildren().size() == 0;
  }

  private void fillInCodeSystems(Code code) {
    codeRepository
        .getCodeSystem(code.getCodeSystem().getUri())
        .ifPresent(codeSystem -> code.setCodeSystem(codeSystem));
    Optional.ofNullable(code.getChildren())
        .ifPresent(children -> children.forEach(child -> fillInCodeSystems(child)));
  }
}
