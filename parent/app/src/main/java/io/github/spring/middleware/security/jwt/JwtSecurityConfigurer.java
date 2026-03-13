package io.github.spring.middleware.security.jwt;

import io.github.spring.middleware.component.NodeInfoRetriever;
import io.github.spring.middleware.security.AbstractAuthorizationSecurityConfigurer;
import io.github.spring.middleware.security.SecurityConfigProperties;
import io.github.spring.middleware.security.SecurityConfigurer;
import io.github.spring.middleware.security.SecurityType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;


public class JwtSecurityConfigurer extends AbstractAuthorizationSecurityConfigurer implements SecurityConfigurer {

    private final SecurityType securityType;
    private final JwtDecoder jwtDecoder;
    private final Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter;

    public JwtSecurityConfigurer(
            SecurityType securityType,
            SecurityConfigProperties configProperties,
            JwtDecoder jwtDecoder,
            Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter,
            NodeInfoRetriever nodeInfoRetriever) {
        super(configProperties, nodeInfoRetriever);
        this.securityType = securityType;
        this.jwtDecoder = jwtDecoder;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    }

    @Override
    public SecurityType securityType() {
        return securityType;
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(this::authorizationRequests)
                .oauth2ResourceServer(oauth2 -> {
                    oauth2.jwt(jwt -> {
                        jwt.decoder(jwtDecoder);
                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter);
                    });
                });
    }
}

