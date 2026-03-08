package io.github.spring.middleware.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

public interface SecurityConfigurer {

    SecurityType securityType();
    void configure(HttpSecurity http) throws Exception;

}
