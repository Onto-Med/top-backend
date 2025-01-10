package care.smith.top.backend.configuration;

import care.smith.top.backend.service.UserService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.server.resource.BearerTokenAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationProvider implements AuthenticationProvider {
  private final JwtDecoder jwtDecoder;
  private final UserService userService;

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

    return new AuthenticatedUser(userService.getOrCreateUser(jwt).orElse(null));
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return authentication.equals(BearerTokenAuthenticationToken.class);
  }
}
