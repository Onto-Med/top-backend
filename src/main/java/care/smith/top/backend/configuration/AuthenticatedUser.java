package care.smith.top.backend.configuration;

import care.smith.top.backend.model.jpa.UserDao;
import java.util.Collection;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public class AuthenticatedUser implements Authentication {
  private final UserDao userDao;

  public AuthenticatedUser(UserDao userDao) {
    this.userDao = userDao;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return userDao.getAuthorities();
  }

  @Override
  public Object getCredentials() {
    return null;
  }

  @Override
  public Object getDetails() {
    return null;
  }

  @Override
  public Object getPrincipal() {
    return userDao;
  }

  @Override
  public boolean isAuthenticated() {
    return userDao != null;
  }

  @Override
  public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {}

  @Override
  public String getName() {
    return null;
  }
}
