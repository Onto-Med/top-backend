package care.smith.top.backend.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public enum Role {
  USER,
  ADMIN;

  public static Role defaultValue() {
    return Role.USER;
  }

  public GrantedAuthority toGrantedAuthority() {
    return new SimpleGrantedAuthority("ROLE_" + name());
  }
}
