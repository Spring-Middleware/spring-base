package io.github.spring.middleware.security;

import io.micrometer.common.util.StringUtils;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

public abstract class AbstractAuthorizationSecurityConfigurer {

    protected final SecurityConfigProperties configProperties;

    protected AbstractAuthorizationSecurityConfigurer(SecurityConfigProperties configProperties) {
        this.configProperties = configProperties;
    }

    protected void authorizationRequests(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        configurePublicPaths(auth);
        configureProtectedPaths(auth);
        auth.anyRequest().authenticated();
    }

    private void configurePublicPaths(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        if (configProperties.getPublicPaths() == null || configProperties.getPublicPaths().isEmpty()) {
            return;
        }
        auth.requestMatchers(configProperties.getPublicPaths().toArray(new String[0])).permitAll();
    }

    private void configureProtectedPaths(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        if (configProperties.getProtectedPaths() == null || configProperties.getProtectedPaths().isEmpty()) {
            return;
        }

        configProperties.getProtectedPaths().stream()
                .filter(this::isValidProtectedPathRule)
                .forEach(rule -> configureProtectedPathRule(auth, rule));
    }

    private boolean isValidProtectedPathRule(SecurityConfigProperties.ProtectedPathRule pathRule) {
        return pathRule.isEnabled()
                && pathRule.getAllowedRoles() != null
                && !pathRule.getAllowedRoles().isEmpty()
                && StringUtils.isNotBlank(pathRule.getPath());
    }

    private void configureProtectedPathRule(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth,
                                            SecurityConfigProperties.ProtectedPathRule pathRule) {
        String[] allowedRoles = pathRule.getAllowedRoles().toArray(new String[0]);

        if (pathRule.getMethods() == null || pathRule.getMethods().isEmpty()) {
            configureRuleWithoutMethods(auth, pathRule, allowedRoles);
        } else {
            configureRuleWithMethods(auth, pathRule, allowedRoles);
        }
    }

    private void configureRuleWithoutMethods(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth,
                                             SecurityConfigProperties.ProtectedPathRule pathRule,
                                             String[] allowedRoles) {
        if (pathRule.getQueryParams() == null || pathRule.getQueryParams().isEmpty()) {
            auth.requestMatchers(pathRule.getPath()).hasAnyRole(allowedRoles);
        } else {
            auth.requestMatchers(new QueryParamPathRequestMatcher(
                    null,
                    pathRule.getPath(),
                    pathRule.getQueryParams()
            )).hasAnyRole(allowedRoles);
        }
    }

    private void configureRuleWithMethods(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth,
                                          SecurityConfigProperties.ProtectedPathRule pathRule,
                                          String[] allowedRoles) {
        pathRule.getMethods().forEach(method -> {
            if (pathRule.getQueryParams() == null || pathRule.getQueryParams().isEmpty()) {
                auth.requestMatchers(method, pathRule.getPath()).hasAnyRole(allowedRoles);
            } else {
                auth.requestMatchers(new QueryParamPathRequestMatcher(
                        method,
                        pathRule.getPath(),
                        pathRule.getQueryParams()
                )).hasAnyRole(allowedRoles);
            }
        });
    }
}
