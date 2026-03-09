package io.github.spring.middleware.security;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

public interface ProtectedPathRuleResolver {
    Optional<SecurityConfigProperties.ProtectedPathRule> resolve(HttpServletRequest request);
}
