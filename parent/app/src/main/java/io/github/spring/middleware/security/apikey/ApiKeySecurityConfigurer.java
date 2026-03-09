package io.github.spring.middleware.security.apikey;

import io.github.spring.middleware.security.AbstractAuthorizationSecurityConfigurer;
import io.github.spring.middleware.security.SecurityConfigProperties;
import io.github.spring.middleware.security.SecurityConfigurer;
import io.github.spring.middleware.security.SecurityType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "middleware.security", name = "type", havingValue = "API_KEY")
public class ApiKeySecurityConfigurer extends AbstractAuthorizationSecurityConfigurer implements SecurityConfigurer {

    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    protected ApiKeySecurityConfigurer(SecurityConfigProperties configProperties, ApiKeyAuthenticationFilter apiKeyAuthenticationFilter) {
        super(configProperties);
        this.apiKeyAuthenticationFilter = apiKeyAuthenticationFilter;
    }


    @Override
    public SecurityType securityType() {
        return SecurityType.API_KEY;
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(apiKeyAuthenticationFilter, AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(this::authorizationRequests);
    }
}
