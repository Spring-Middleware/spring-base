package io.github.spring.middleware.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableConfigurationProperties(SecurityConfigProperties.class)
@EnableMethodSecurity(jsr250Enabled = true)
public class MiddlewareSecurityConfiguration {

    private final Map<SecurityType, SecurityConfigurer> configurers;

    public MiddlewareSecurityConfiguration(List<SecurityConfigurer> configurers) {
        this.configurers = configurers.stream()
                .collect(Collectors.toMap(
                        SecurityConfigurer::securityType,
                        c -> c,
                        (a, b) -> {
                            throw new IllegalStateException(
                                    STR."Multiple SecurityConfigurer beans found for type: \{a.securityType()}"
                            );
                        }
                ));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityConfigProperties properties) throws Exception {
        if (properties.getType() == null || properties.getType() == SecurityType.NONE) {
            http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        SecurityConfigurer configurer = configurers.get(properties.getType());
        if (configurer == null) {
            throw new IllegalStateException(
                    STR."No SecurityConfigurer found for type: \{properties.getType()}"
            );
        }
        configurer.configure(http);
        return http.build();
    }

}