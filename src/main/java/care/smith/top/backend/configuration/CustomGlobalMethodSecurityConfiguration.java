package care.smith.top.backend.configuration;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.interceptor.SimpleTraceInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.method.MethodSecurityMetadataSource;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;

@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class CustomGlobalMethodSecurityConfiguration extends GlobalMethodSecurityConfiguration {
  @Value("${spring.security.oauth2.enabled}")
  private Boolean oauth2Enabled;

  public MethodInterceptor methodSecurityInterceptor(
      MethodSecurityMetadataSource methodSecurityMetadataSource) {
    return oauth2Enabled
        ? super.methodSecurityInterceptor(methodSecurityMetadataSource)
        : new SimpleTraceInterceptor();
  }
}
