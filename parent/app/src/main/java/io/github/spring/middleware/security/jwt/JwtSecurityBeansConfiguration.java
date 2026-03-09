package io.github.spring.middleware.security.jwt;

import io.github.spring.middleware.security.SecurityConfigProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Configuration
public class JwtSecurityBeansConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "middleware.security", name = "type", havingValue = "JWT")
    public JwtDecoder jwtDecoder(SecurityConfigProperties properties) {
        SecurityConfigProperties.Jwt jwt = Optional.ofNullable(properties.getJwt())
                .orElseThrow(() -> new IllegalArgumentException("JWT configuration is missing"));

        String secret = Optional.ofNullable(jwt.getSecret())
                .orElseThrow(() -> new IllegalArgumentException("JWT secret is not configured"));

        SecretKeySpec keySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                jwt.getAlgorithm().javaName()
        );

        return NimbusJwtDecoder.withSecretKey(keySpec).build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "middleware.security", name = "type", havingValue = "OIDC")
    public JwtDecoder oidcDecoder(SecurityConfigProperties properties) {
        SecurityConfigProperties.Oidc oidc = Optional.ofNullable(properties.getOidc())
                .orElseThrow(() -> new IllegalArgumentException("OIDC configuration is missing"));

        String issuerUri = Optional.ofNullable(oidc.getIssuerUri())
                .orElseThrow(() -> new IllegalArgumentException("OIDC issuer URI is not configured"));

        if (oidc.getJwkSetUri() != null && !oidc.getJwkSetUri().isBlank()) {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(oidc.getJwkSetUri()).build();
            decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));
            return decoder;
        }

        return JwtDecoders.fromIssuerLocation(issuerUri);
    }

    @Bean
    public Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter(
            SecurityConfigProperties properties) {
        return new JsonPathJwtAuthenticationConverter(properties);
    }
}
