package care.smith.top.backend.configuration;

import org.springframework.security.authentication.AnonymousAuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;

import java.util.UUID;

public class JwtResourceServerCustomizer
    implements Customizer<OAuth2ResourceServerConfigurer<HttpSecurity>.JwtConfigurer> {
  private final JwtAuthenticationProvider customAuthenticationProvider;

  public JwtResourceServerCustomizer(JwtAuthenticationProvider customAuthenticationProvider) {
    this.customAuthenticationProvider = customAuthenticationProvider;
  }

  @Override
  public void customize(OAuth2ResourceServerConfigurer<HttpSecurity>.JwtConfigurer jwtConfigurer) {
    String key = UUID.randomUUID().toString();
    AnonymousAuthenticationProvider anonymousAuthenticationProvider =
        new AnonymousAuthenticationProvider(key);
    ProviderManager providerManager =
        new ProviderManager(this.customAuthenticationProvider, anonymousAuthenticationProvider);
    jwtConfigurer.authenticationManager(providerManager);
  }
}
