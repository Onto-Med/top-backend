package care.smith.top.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import care.smith.top.backend.model.jpa.OrganisationDao;
import care.smith.top.backend.model.jpa.Permission;
import care.smith.top.backend.model.jpa.UserDao;
import care.smith.top.backend.model.jpa.key.OrganisationMembershipKeyDao;
import java.util.Collections;
import org.junit.jupiter.api.Test;

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

    assertThat(organisationService.getMemberships(organisation.getId())).size().isEqualTo(1);

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
