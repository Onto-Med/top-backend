package care.smith.top.backend.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public enum Role {
  ANONYMOUS,
  USER,
  ADMIN;

  public static Role defaultValue() {
    return Role.USER;
  }

  public GrantedAuthority toGrantedAuthority() {
    return new SimpleGrantedAuthority("ROLE_" + name());
  }

  public Collection<? extends GrantedAuthority> toGrantedAuthorities() {
    return getIncludingLower().stream().map(Role::toGrantedAuthority).collect(Collectors.toList());
  }

  /**
   * Create a list of roles that are higher or lower than this role, depending on the parameter
   * {@code ascending}. The list will always at least contain this role, the method is called on.
   *
   * <p>For example the resulting list of {@code Role.USER.getIncluding(true)} contains: {@code
   * Role.USER} and {@code Role.ADMIN}.
   *
   * @param ascending If true, all roles that are higher than this role are included in the
   *     resulting list. Otherwise, all lower roles are included.
   * @return A list of roles.
   */
  public List<Role> getIncluding(boolean ascending) {
    return Arrays.stream(Role.values())
        .filter(p -> ascending ? p.ordinal() >= ordinal() : p.ordinal() <= ordinal())
        .collect(Collectors.toList());
  }

  public List<Role> getIncludingHigher() {
    return this.getIncluding(true);
  }

  public List<Role> getIncludingLower() {
    return this.getIncluding(false);
  }
}
