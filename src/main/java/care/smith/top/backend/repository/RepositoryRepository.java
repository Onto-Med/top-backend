package care.smith.top.backend.repository;

import care.smith.top.backend.model.*;
import care.smith.top.model.RepositoryType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public interface RepositoryRepository
    extends JpaRepository<RepositoryDao, String>, JpaSpecificationExecutor<RepositoryDao> {
  boolean existsByIdAndOrganisation_Id(String id, String id1);

  static Specification<RepositoryDao> byPrimary(@Nullable Boolean primary) {
    return (root, query, cb) -> {
      if (primary == null) return cb.and();
      return cb.isTrue(root.get(RepositoryDao_.PRIMARY));
    };
  }

  static Specification<RepositoryDao> byRepositoryType(RepositoryType repositoryType) {
    return byRepositoryType(
        repositoryType == null ? null : Collections.singletonList(repositoryType));
  }

  static Specification<RepositoryDao> byRepositoryType(List<RepositoryType> repositoryTypes) {
    return (root, query, cb) -> {
      if (repositoryTypes == null || repositoryTypes.isEmpty()) return cb.and();
      return root.get(RepositoryDao_.REPOSITORY_TYPE).in(repositoryTypes);
    };
  }

  static Specification<RepositoryDao> byName(String name) {
    return (root, query, cb) -> {
      if (name == null) return cb.and();
      return cb.like(cb.lower(root.get(RepositoryDao_.NAME)), "%" + name.toLowerCase() + "%");
    };
  }

  static Specification<RepositoryDao> byOrganisationId(String organisationId) {
    return (root, query, cb) -> {
      if (organisationId == null) return cb.and();
      return cb.equal(
          root.join(RepositoryDao_.ORGANISATION).get(OrganisationDao_.ID), organisationId);
    };
  }

  /**
   * This filter does the following:
   *
   * <ul>
   *   <li>evaluates to true, if user is {@code null} or has role {@code Role.ADMIN}
   *   <li>evaluates to true, if the repository is primary
   *   <li>checks if the user has an organisation membership relation to the organisation the
   *       repository belongs to
   * </ul>
   *
   * @param user The user to filter by.
   * @return A specification for Domain Driven Design.
   */
  static Specification<RepositoryDao> byUser(UserDao user) {
    return (root, query, cb) -> {
      if (user == null || user.getRole().equals(Role.ADMIN)) return cb.and();
      return cb.or(
          cb.isTrue(root.get(RepositoryDao_.PRIMARY)),
          cb.equal(
              root.join(RepositoryDao_.ORGANISATION)
                  .join(OrganisationDao_.MEMBERS)
                  .join(OrganisationMembershipDao_.USER)
                  .get(UserDao_.ID),
              user.getId()));
    };
  }

  default Slice<RepositoryDao> findByOrganisationIdAndNameAndPrimaryAndRepositoryType(
      String organisationId,
      String name,
      Boolean primary,
      RepositoryType repositoryType,
      UserDao user,
      Pageable pageable) {

    return findAll(
        byOrganisationId(organisationId)
            .and(byName(name))
            .and(byPrimary(primary))
            .and(byRepositoryType(repositoryType))
            .and(byUser(user)),
        pageable);
  }

  Optional<RepositoryDao> findByIdAndOrganisationId(String repositoryId, String organisationId);
}
