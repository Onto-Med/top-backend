package care.smith.top.backend.service;

import care.smith.top.backend.model.OrganisationDao;
import care.smith.top.backend.model.Permission;
import care.smith.top.backend.model.UserDao;
import care.smith.top.backend.model.key.OrganisationMembershipKeyDao;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class UserServiceTest extends AbstractTest {
  @Test
  void grantAndRevokeMembership() {
    OrganisationDao organisation =
        organisationRepository.save(new OrganisationDao("organisation", "organisation", null));
    UserDao user = userRepository.save(new UserDao("user_id", "username"));

    assertThat(
            userService.hasPermission(
                user, organisation.getId(), organisation.getClass().getName(), Permission.READ))
        .isFalse();

    userService.grantMembership(organisation, user, Permission.READ);
    assertThat(
            userService.hasPermission(
                user, organisation.getId(), organisation.getClass().getName(), Permission.READ))
        .isTrue();

    userService.revokeMembership(organisation, user);
    assertThat(
            userService.hasPermission(
                user, organisation.getId(), organisation.getClass().getName(), Permission.READ))
        .isFalse();
  }

  @Test
  void hasPermission() {
    OrganisationDao organisation =
        organisationRepository.save(new OrganisationDao("organisation", "organisation", null));
    UserDao user = userRepository.save(new UserDao("user_id", "username"));

    assertThat(
            organisationMembershipRepository.findById(
                new OrganisationMembershipKeyDao(user.getId(), organisation.getId())))
        .isEmpty();
    assertThat(
            userService.hasPermission(
                user, organisation.getId(), organisation.getClass().getName(), Permission.READ))
        .isFalse();

    organisation.setMemberPermission(user, Permission.READ);
    organisation = organisationRepository.save(organisation);

    assertThat(
            organisationMembershipRepository.findById(
                new OrganisationMembershipKeyDao(user.getId(), organisation.getId())))
        .isNotEmpty();
    assertThat(
            userService.hasPermission(
                user, organisation.getId(), organisation.getClass().getName(), Permission.READ))
        .isTrue();
  }

  @Test
  void getUserById() {
    UserDao user = userRepository.save(new UserDao("user_id", "username"));
    assertThat(userService.getUserById("invalid")).isEmpty();
    assertThat(userService.getUserById(user.getId()))
        .isNotEmpty()
        .get()
        .satisfies(
            u -> {
              assertThat(u.getId()).isEqualTo(user.getId());
              assertThat(u.getUsername()).isEqualTo(user.getUsername());
              assertThat(u.getOrganisations()).isEmpty();
            });
  }

  @Test
  void getUsers() {
    UserDao user1 = userRepository.save(new UserDao("user_id_1", "username_1"));
    UserDao user2 = userRepository.save(new UserDao("user_id_2", "username_2"));
    OrganisationDao organisation = new OrganisationDao("organisation", "organisation", null);
    organisation.setMemberPermission(user1, Permission.READ);
    organisation = organisationRepository.save(organisation);

    assertThat(userService.getUsers(null, null, 1)).isNotNull().size().isEqualTo(2);
    assertThat(userService.getUsers("2", null, 1))
        .isNotNull()
        .allSatisfy(u -> assertThat(u.getId()).isEqualTo(user2.getId()))
        .size()
        .isEqualTo(1);
    assertThat(userService.getUsers(null, Collections.singletonList(organisation.getId()), 1))
        .isNotNull()
        .allSatisfy(u -> assertThat(u.getId()).isEqualTo(user1.getId()))
        .size()
        .isEqualTo(1);
  }
}
