package care.smith.top.backend;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import care.smith.top.model.*;
import org.junit.jupiter.api.Test;

public class EnumTest {
  /**
   * This test checks if enumerations provided by top-api have changed. Because these enumerations
   * are stored as {@link javax.persistence.Enumerated} with type ORDINAL, rearranging them will
   * lead to data inconsistency.
   *
   * <p>Measures required if this test fails for an enumeration:
   *
   * <ul>
   *   <li>enumeration values have been renamed: update the hardcoded values in this test
   *   <li>enumeration values were rearranged: create a liquibase migration script
   * </ul>
   */
  @Test
  void testEnumsDoNotHaveBreakingChanges() {
    assertThat(Role.values()).isEqualTo(new Role[] {Role.ADMIN, Role.USER});

    assertThat(DataType.values())
        .isEqualTo(
            new DataType[] {
              DataType.STRING, DataType.NUMBER, DataType.BOOLEAN, DataType.DATE_TIME
            });

    assertThat(EntityType.values())
        .isEqualTo(
            new EntityType[] {
              EntityType.CATEGORY,
              EntityType.SINGLE_CONCEPT,
              EntityType.COMPOSITE_CONCEPT,
              EntityType.SINGLE_PHENOTYPE,
              EntityType.COMPOSITE_PHENOTYPE,
              EntityType.SINGLE_RESTRICTION,
              EntityType.COMPOSITE_RESTRICTION
            });

    assertThat(ItemType.values())
        .isEqualTo(
            new ItemType[] {
              ItemType.ALLERGY_INTOLERANCE,
              ItemType.CLINICAL_IMPRESSION,
              ItemType.CONDITION,
              ItemType.ENCOUNTER,
              ItemType.MEDICATION,
              ItemType.MEDICATION_ADMINISTRATION,
              ItemType.MEDICATION_REQUEST,
              ItemType.MEDICATION_STATEMENT,
              ItemType.OBSERVATION,
              ItemType.PROCEDURE,
              ItemType.SUBJECT_AGE,
              ItemType.SUBJECT_BIRTH_DATE,
              ItemType.SUBJECT_SEX
            });

    assertThat(QueryState.values())
        .isEqualTo(
            new QueryState[] {
              QueryState.FAILED, QueryState.FINISHED, QueryState.QUEUED, QueryState.RUNNING
            });

    assertThat(RepositoryType.values())
        .isEqualTo(
            new RepositoryType[] {
              RepositoryType.PHENOTYPE_REPOSITORY, RepositoryType.CONCEPT_REPOSITORY
            });

    assertThat(Quantifier.values())
        .isEqualTo(
            new Quantifier[] {Quantifier.ALL, Quantifier.EXACT, Quantifier.MIN, Quantifier.MAX});

    assertThat(RestrictionOperator.values())
        .isEqualTo(
            new RestrictionOperator[] {
              RestrictionOperator.LESS_THAN,
              RestrictionOperator.LESS_THAN_OR_EQUAL_TO,
              RestrictionOperator.GREATER_THAN,
              RestrictionOperator.GREATER_THAN_OR_EQUAL_TO
            });

    assertThat(CodeScope.values())
        .isEqualTo(new CodeScope[] {CodeScope.SELF, CodeScope.SUBTREE, CodeScope.LEAVES});
  }
}
