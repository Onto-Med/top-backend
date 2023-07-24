package care.smith.top.backend.repository.jpa;

import care.smith.top.backend.model.jpa.OrganisationDao_;
import care.smith.top.backend.model.jpa.OrganisationMembershipDao_;
import care.smith.top.backend.model.jpa.UserDao;
import care.smith.top.backend.model.jpa.UserDao_;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository
    extends JpaRepository<UserDao, String>, JpaSpecificationExecutor<UserDao> {
  static Specification<UserDao> byUsername(@Nullable String username) {
    return (root, query, cb) -> {
      if (username == null) return cb.and();
      return cb.like(cb.lower(root.get(UserDao_.USERNAME)), "%" + username.toLowerCase() + "%");
    };
  }

  static Specification<UserDao> byOrganisationId(@Nullable String organisationId) {
    return byOrganisationIds(Collections.singletonList(organisationId));
  }

  static Specification<UserDao> byOrganisationIds(List<String> organisationIds) {
    return (root, query, cb) -> {
      if (organisationIds == null || organisationIds.isEmpty()) return cb.and();
      query.distinct(true); // This may cause side effects!
      return root.join(UserDao_.MEMBERSHIPS)
          .join(OrganisationMembershipDao_.ORGANISATION)
          .get(OrganisationDao_.ID)
          .in(organisationIds);
    };
  }

  Optional<UserDao> findByUsername(String username);

  default Page<UserDao> findAllByUsernameAndOrganisationIds(
      String username, List<String> organisationIds, Pageable pageable) {
    return findAll(byUsername(username).and(byOrganisationIds(organisationIds)), pageable);
  }
}
