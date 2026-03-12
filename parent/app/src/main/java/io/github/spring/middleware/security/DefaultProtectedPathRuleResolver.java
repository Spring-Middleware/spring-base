package io.github.spring.middleware.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Optional;

@Component
public class DefaultProtectedPathRuleResolver implements ProtectedPathRuleResolver {

    private final SecurityConfigProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public DefaultProtectedPathRuleResolver(SecurityConfigProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<SecurityConfigProperties.ProtectedPathRule> resolve(HttpServletRequest request) {

        List<SecurityConfigProperties.ProtectedPathRule> rules = properties.getProtectedPaths();

        if (CollectionUtils.isEmpty(rules)) {
            return Optional.empty();
        }

        String requestPath = request.getServletPath();
        HttpMethod requestMethod = HttpMethod.valueOf(request.getMethod());

        for (SecurityConfigProperties.ProtectedPathRule rule : rules) {

            if (rule.getType() == null || rule.getType() != SecurityPathType.NONE) {
                continue;
            }

            if (!matchesMethod(rule, requestMethod)) {
                continue;
            }

            if (!matchesPath(rule, requestPath)) {
                continue;
            }

            return Optional.of(rule); // first match wins
        }

        return Optional.empty();
    }

    private boolean matchesPath(SecurityConfigProperties.ProtectedPathRule rule, String requestPath) {
        return pathMatcher.match(rule.getPath(), requestPath);
    }

    private boolean matchesMethod(SecurityConfigProperties.ProtectedPathRule rule, HttpMethod requestMethod) {

        List<HttpMethod> methods = rule.getMethods();

        if (CollectionUtils.isEmpty(methods)) {
            return true; // rule applies to all methods
        }

        return methods.contains(requestMethod);
    }
}
