package care.smith.top.backend.api;

import care.smith.top.backend.model.UserDao;
import care.smith.top.backend.service.UserService;
import care.smith.top.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserApiDelegateImpl implements UserApiDelegate {
  @Autowired UserService userService;

  @Override
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<User> getUserById(String userId) {
    Optional<User> user = userService.getUserById(userId).map(UserDao::toApiModel);
    return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
  }

  @Override
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<List<User>> getUsers(
      String name, List<String> organisationIds, Integer page) {
    return ResponseEntity.ok(
        userService.getUsers(name, organisationIds, page).stream()
            .map(UserDao::toApiModel)
            .collect(Collectors.toList()));
  }
}
