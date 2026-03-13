package io.github.spring.middleware.security.jwt;

import io.github.spring.middleware.security.SecurityConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.util.StringUtils;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
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

        String issuerUri = oidc.getIssuerUri();
        String jwkSetUri = oidc.getJwkSetUri();

        if (StringUtils.hasText(jwkSetUri)) {

            NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

            if (StringUtils.hasText(issuerUri)) {
                decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));
            } else {
                log.warn("OIDC issuer-uri is not configured. JWT validation will verify signature only (issuer will NOT be validated).");
                decoder.setJwtValidator(JwtValidators.createDefault());
            }

            return decoder;
        }

        if (StringUtils.hasText(issuerUri)) {
            return JwtDecoders.fromIssuerLocation(issuerUri);
        }

        throw new IllegalArgumentException(
                "Invalid OIDC configuration: either 'issuer-uri' or 'jwk-set-uri' must be configured"
        );
    }

    @Bean
    public Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter(
            SecurityConfigProperties properties) {
        return new JsonPathJwtAuthenticationConverter(properties);
    }
}
