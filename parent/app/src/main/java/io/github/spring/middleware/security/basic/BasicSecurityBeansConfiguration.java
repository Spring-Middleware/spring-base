package io.github.spring.middleware.security.basic;

import io.github.spring.middleware.security.SecurityConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "middleware.security", name = "type", havingValue = "BASIC_AUTH")
public class BasicSecurityBeansConfiguration {

    private final SecurityConfigProperties configProperties;
    private final Optional<UserApi> userApi;

    @Bean
    public UserDetailsService userDetailsService() {

        if (userApi.isPresent() && configProperties.getBasicAuth() != null && configProperties.getBasicAuth().getUserApi().isEnabled()) {
            return userApi.get();
        }

        if (configProperties.getBasicAuth() != null && configProperties.getBasicAuth().getCredentials() != null) {
            List<UserDetails> userDetailsList = configProperties.getBasicAuth().getCredentials().stream().map(credentials -> {
                UserDetails userDetails = null;
                if (credentials.getUsername() == null || credentials.getPassword() == null) {
                    log.warn("Invalid basic auth credentials configuration: username and password must be provided. Skipping entry");
                } else {
                    userDetails = User.withUsername(credentials.getUsername())
                            .password(passwordEncoder().encode(credentials.getPassword()))
                            .roles(credentials.getRoles().toArray(new String[0]))
                            .build();
                }
                return userDetails;
            }).filter(Objects::nonNull).toList();
            if (userDetailsList.isEmpty()) {
                throw new IllegalArgumentException("No valid user credentials provided for basic authentication. " +
                        "Please provide valid credentials in configuration or implement a UserApi.");
            }
            return new InMemoryUserDetailsManager(userDetailsList);
        } else {
            throw new IllegalArgumentException("No user details service configured for basic authentication. " +
                    "Please provide credentials in configuration or implement a UserApi.");
        }
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
