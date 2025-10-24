package care.smith.top.backend.repository.jpa;

import care.smith.top.backend.model.jpa.CodeDao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface CodeRepository
    extends JpaRepository<CodeDao, Long>, JpaSpecificationExecutor<CodeDao> {

  @Query(
      nativeQuery = true,
      value =
          "WITH RECURSIVE code_tree AS ( "
              + "  SELECT id root_id, 0 level, code.* "
              + "  FROM entity_version_codes "
              + "    JOIN code ON (codes_id = code.id) "
              + "  WHERE entity_version_id = :entityVersionId "
              + "  UNION ALL "
              + "  SELECT root_id, level + 1 level, c.* "
              + "  FROM code_tree ct "
              + "    JOIN code c ON (ct.id = c.parent_id) "
              + ") "
              + "SELECT * "
              + "FROM code_tree "
              + "WHERE CASE WHEN :levelCap IS NULL THEN true ELSE level <= :levelCap END "
              + "ORDER BY parent_id NULLS FIRST, children_order")
  List<CodeDao> getCodeTreeByEntityVersionId(Long entityVersionId, Integer levelCap);
}
