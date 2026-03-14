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
    @ConditionalOnProperty(prefix = "middleware.security", name = "type", havingValue = "OAUTH2")
    public JwtDecoder oauth2Decoder(SecurityConfigProperties properties) {

        SecurityConfigProperties.Oauth2 oauth2 = Optional.ofNullable(properties.getOauth2())
                .orElseThrow(() -> new IllegalArgumentException("OATUH2 configuration is missing"));

        String issuerUri = oauth2.getIssuerUri();
        String jwkSetUri = oauth2.getJwkSetUri();

        if (StringUtils.hasText(jwkSetUri)) {

            NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

            if (StringUtils.hasText(issuerUri)) {
                decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));
            } else {
                log.warn("OAUTH2 issuer-uri not configured. Token issuer will NOT be validated. This configuration should only be used in trusted environments.");
                decoder.setJwtValidator(JwtValidators.createDefault());
            }

            return decoder;
        }

        if (StringUtils.hasText(issuerUri)) {
            return JwtDecoders.fromIssuerLocation(issuerUri);
        }

        throw new IllegalArgumentException(
                "Invalid OAUTH2 configuration: either 'issuer-uri' or 'jwk-set-uri' must be configured to validate JWT tokens."
        );
    }

    @Bean
    public Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter(
            SecurityConfigProperties properties) {
        return new JsonPathJwtAuthenticationConverter(properties);
    }
}
