package care.smith.top.backend.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity(prePostEnabled = false, securedEnabled = true)
@ConditionalOnProperty(name = "spring.security.oauth2.enabled", havingValue = "true")
public class CustomMethodSecurityConfiguration {}
