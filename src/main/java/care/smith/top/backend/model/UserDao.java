package care.smith.top.backend.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

@Entity(name = "user_table")
public class UserDao implements UserDetails {
  @Id private String id;
  private String username;
  private boolean enabled = true;
  private boolean locked = false;
  private Date expirationDate;

  @Enumerated(EnumType.STRING)
  private Role role = Role.USER;

  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private Collection<OrganisationMembershipDao> memberships = new ArrayList<>();

  public UserDao() {}

  public UserDao(@NotNull String id, String username) {
    this.id = id;
    this.username = username;
  }

  public UserDao id(String id) {
    this.id = id;
    return this;
  }

  public UserDao enabled(boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  public UserDao locked(boolean locked) {
    this.locked = locked;
    return this;
  }

  public UserDao expirationDate(Date expirationDate) {
    this.expirationDate = expirationDate;
    return this;
  }

  public UserDao username(String username) {
    this.username = username;
    return this;
  }

  public UserDao role(Role role) {
    this.role = role;
    return this;
  }

  public UserDao memberships(Collection<OrganisationMembershipDao> memberships) {
    this.memberships = memberships;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    UserDao userDao = (UserDao) o;

    if (isEnabled() != userDao.isEnabled()) return false;
    if (locked != userDao.locked) return false;
    if (getId() != null ? !getId().equals(userDao.getId()) : userDao.getId() != null) return false;
    if (getUsername() != null
        ? !getUsername().equals(userDao.getUsername())
        : userDao.getUsername() != null) return false;
    return getExpirationDate() != null
        ? getExpirationDate().equals(userDao.getExpirationDate())
        : userDao.getExpirationDate() == null;
  }

  @Override
  public int hashCode() {
    int result = getId() != null ? getId().hashCode() : 0;
    result = 31 * result + (getUsername() != null ? getUsername().hashCode() : 0);
    result = 31 * result + (isEnabled() ? 1 : 0);
    result = 31 * result + (locked ? 1 : 0);
    result = 31 * result + (getExpirationDate() != null ? getExpirationDate().hashCode() : 0);
    return result;
  }

  public String getId() {
    return id;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return Collections.singleton(role.toGrantedAuthority());
  }

  @Override
  public String getPassword() {
    return null;
  }

  public String getUsername() {
    return username;
  }

  public Role getRole() {
    return role;
  }

  public Date getExpirationDate() {
    return expirationDate;
  }

  @Override
  public boolean isAccountNonExpired() {
    return expirationDate == null || expirationDate.after(new Date());
  }

  @Override
  public boolean isAccountNonLocked() {
    return !locked;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return false;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  public Collection<OrganisationMembershipDao> getMemberships() {
    return memberships;
  }
}
