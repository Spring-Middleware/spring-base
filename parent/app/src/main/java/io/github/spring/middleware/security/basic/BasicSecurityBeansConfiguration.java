package io.github.spring.middleware.security.basic;

import io.github.spring.middleware.security.SecurityConfigProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import java.util.Optional;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "middleware.security", name = "type", havingValue = "BASIC_AUTH")
public class BasicSecurityBeansConfiguration {

    private final SecurityConfigProperties configProperties;
    private final Optional<UserApi> userApi;

    @Bean
    public UserDetailsService userDetailsService() {

        if (userApi.isPresent()) {
            return userApi.get();
        }

        if (configProperties.getBasic() != null && configProperties.getBasic().getCredentials() != null) {
            UserDetails user = User.withUsername(configProperties.getBasic().getCredentials().getUsername())
                    .password(passwordEncoder().encode(configProperties.getBasic().getCredentials().getPassword()))
                    .roles(configProperties.getBasic().getCredentials().getRoles().toArray(new String[0]))
                    .build();
            return new InMemoryUserDetailsManager(user);
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
