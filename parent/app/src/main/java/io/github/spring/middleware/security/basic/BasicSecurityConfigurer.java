package io.github.spring.middleware.security.basic;

import io.github.spring.middleware.security.AbstractAuthorizationSecurityConfigurer;
import io.github.spring.middleware.security.SecurityConfigProperties;
import io.github.spring.middleware.security.SecurityConfigurer;
import io.github.spring.middleware.security.SecurityType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.stereotype.Component;

@Component
public final class BasicSecurityConfigurer extends AbstractAuthorizationSecurityConfigurer implements SecurityConfigurer {

    public BasicSecurityConfigurer(SecurityConfigProperties configProperties) {
        super(configProperties);
    }


    @Override
    public SecurityType securityType() {
        return SecurityType.BASIC_AUTH;
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(this::authorizationRequests)
                .httpBasic(Customizer.withDefaults());
    }
}