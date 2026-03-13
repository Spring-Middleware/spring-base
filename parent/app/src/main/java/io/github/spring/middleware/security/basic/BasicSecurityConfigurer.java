package io.github.spring.middleware.security.basic;

import io.github.spring.middleware.component.NodeInfoRetriever;
import io.github.spring.middleware.security.AbstractAuthorizationSecurityConfigurer;
import io.github.spring.middleware.security.SecurityConfigProperties;
import io.github.spring.middleware.security.SecurityConfigurer;
import io.github.spring.middleware.security.SecurityType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "middleware.security", name = "type", havingValue = "BASIC_AUTH")
public final class BasicSecurityConfigurer extends AbstractAuthorizationSecurityConfigurer implements SecurityConfigurer {

    public BasicSecurityConfigurer(SecurityConfigProperties configProperties, NodeInfoRetriever nodeInfoRetriever) {
        super(configProperties, nodeInfoRetriever);
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