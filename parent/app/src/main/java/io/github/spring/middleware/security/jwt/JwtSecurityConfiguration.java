package io.github.spring.middleware.security.jwt;

import io.github.spring.middleware.component.NodeInfoRetriever;
import io.github.spring.middleware.security.SecurityConfigProperties;
import io.github.spring.middleware.security.SecurityConfigurer;
import io.github.spring.middleware.security.SecurityType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@Configuration
public class JwtSecurityConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "middleware.security", name = "type", havingValue = "JWT")
    public SecurityConfigurer jwtSecurityConfigurer(
            SecurityConfigProperties properties,
            JwtDecoder jwtDecoder,
            Converter<Jwt, ? extends AbstractAuthenticationToken> converter,
            NodeInfoRetriever nodeInfoRetriever) {

        return new JwtSecurityConfigurer(
                SecurityType.JWT,
                properties,
                jwtDecoder,
                converter,
                nodeInfoRetriever
        );
    }

    @Bean
    @ConditionalOnProperty(prefix = "middleware.security", name = "type", havingValue = "OIDC")
    public SecurityConfigurer oidcSecurityConfigurer(
            SecurityConfigProperties properties,
            JwtDecoder oidcJwtDecoder,
            Converter<Jwt, ? extends AbstractAuthenticationToken> converter,
            NodeInfoRetriever nodeInfoRetriever) {

        return new JwtSecurityConfigurer(
                SecurityType.OIDC,
                properties,
                oidcJwtDecoder,
                converter,
                nodeInfoRetriever
        );
    }

}
