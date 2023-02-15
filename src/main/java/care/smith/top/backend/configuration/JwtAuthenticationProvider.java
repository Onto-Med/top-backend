package care.smith.top.backend.configuration;

import care.smith.top.backend.model.UserDao;
import care.smith.top.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.server.resource.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JwtAuthenticationProvider implements AuthenticationProvider {
  private final JwtDecoder jwtDecoder;
  private final UserService userService;

  @Autowired
  public JwtAuthenticationProvider(JwtDecoder jwtDecoder, UserService userService) {
    this.jwtDecoder = jwtDecoder;
    this.userService = userService;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    BearerTokenAuthenticationToken token = (BearerTokenAuthenticationToken) authentication;

    Jwt jwt;
    try {
      jwt = this.jwtDecoder.decode(token.getToken());
    } catch (JwtValidationException ex) {
      return null;
    }

    Optional<UserDao> user = userService.getUser(jwt);

    return user.map(userDao -> new JwtAuthenticationToken(jwt, userDao.getAuthorities()))
        .orElse(null);
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return authentication.equals(BearerTokenAuthenticationToken.class);
  }
}
