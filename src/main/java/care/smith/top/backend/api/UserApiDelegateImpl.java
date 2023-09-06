package care.smith.top.backend.api;

import care.smith.top.backend.model.jpa.UserDao;
import care.smith.top.backend.service.UserService;
import care.smith.top.backend.util.ApiModelMapper;
import care.smith.top.model.User;
import care.smith.top.model.UserPage;
import java.util.List;
import java.util.Optional;
import javax.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

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
  @Transactional
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<UserPage> getUsers(
      String name, List<String> organisationIds, Integer page) {
    return ResponseEntity.ok(
        ApiModelMapper.toUserPage(
            userService.getUsers(name, organisationIds, page).map(UserDao::toApiModel)));
  }
}
