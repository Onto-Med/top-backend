package care.smith.top.backend.util;

import care.smith.top.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiModelMapperTest {

  /**
   * Order should be: Category < Abstract Single Phenotype < Restricted Single Phenotype < [Abstract
   * Composite Phenotype < Restricted Composite Phenotype] The last order should be depending on use
   * in expression.
   */
  @Test
  void compare() {
    Category cat1 = (Category) new Category().id("cat1").entityType(EntityType.CATEGORY);
    Category cat2 = (Category) new Category().id("cat2").entityType(EntityType.CATEGORY);

    assertEquals(-1, ApiModelMapper.compare(cat1, cat2));
    assertEquals(1, ApiModelMapper.compare(cat2, cat1));

    Phenotype abs1 = (Phenotype) new Phenotype().id("abs1").entityType(EntityType.SINGLE_PHENOTYPE);
    Phenotype abs2 =
        (Phenotype) new Phenotype().id("abs2").entityType(EntityType.COMPOSITE_PHENOTYPE);

    assertEquals(-1, ApiModelMapper.compare(cat1, abs1));
    assertEquals(-1, ApiModelMapper.compare(abs1, abs2));
    assertEquals(1, ApiModelMapper.compare(abs2, abs1));

    Phenotype res1 =
        (Phenotype)
            new Phenotype()
                .superPhenotype(abs1)
                .id("res1")
                .entityType(EntityType.SINGLE_RESTRICTION);
    Phenotype res2 =
        (Phenotype)
            new Phenotype()
                .superPhenotype(abs2)
                .id("res2")
                .entityType(EntityType.COMPOSITE_RESTRICTION);

    assertEquals(-1, ApiModelMapper.compare(cat1, res1));
    assertEquals(-1, ApiModelMapper.compare(abs1, res1));
    assertEquals(-1, ApiModelMapper.compare(res1, res2));
    assertEquals(1, ApiModelMapper.compare(res2, res1));

    Phenotype abs3 =
        (Phenotype)
            new Phenotype()
                .expression(new Expression().entityId(abs2.getId()))
                .id("abs")
                .entityType(EntityType.COMPOSITE_PHENOTYPE);

    assertTrue(ApiModelMapper.compare(res1, abs3) < 0);
    assertTrue(ApiModelMapper.compare(abs2, abs3) < 0);
  }
}
