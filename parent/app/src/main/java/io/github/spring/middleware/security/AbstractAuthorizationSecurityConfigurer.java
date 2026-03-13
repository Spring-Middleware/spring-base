package io.github.spring.middleware.security;

import io.github.spring.middleware.component.NodeInfoRetriever;
import io.micrometer.common.util.StringUtils;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

public abstract class AbstractAuthorizationSecurityConfigurer {

    protected final SecurityConfigProperties configProperties;
    protected final NodeInfoRetriever nodeInfoRetriever;

    protected AbstractAuthorizationSecurityConfigurer(SecurityConfigProperties configProperties, NodeInfoRetriever nodeInfoRetriever) {
        this.nodeInfoRetriever = nodeInfoRetriever;
        this.configProperties = configProperties;
    }

    protected void authorizationRequests(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        configurePublicPaths(auth);
        configureProtectedPaths(auth);
        auth.anyRequest().authenticated();
    }

    private void configurePublicPaths(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {

        if (nodeInfoRetriever.getMandatoryPublicPaths() != null && !nodeInfoRetriever.getMandatoryPublicPaths().isEmpty()) {
            auth.requestMatchers(nodeInfoRetriever.getMandatoryPublicPaths().toArray(new String[0])).permitAll();
        }
        if (configProperties.getPublicPaths() == null || configProperties.getPublicPaths().isEmpty()) {
            return;
        }
        auth.requestMatchers(configProperties.getPublicPaths().toArray(new String[0])).permitAll();
    }

    private void configureProtectedPaths(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        if (configProperties.getProtectedPaths() == null || configProperties.getProtectedPaths().isEmpty()) {
            return;
        }

        configProperties.getProtectedPaths().forEach(rule -> configurePathRule(auth, rule));
    }

    private void configurePathRule(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth,
            SecurityConfigProperties.ProtectedPathRule rule) {

        if (!hasValidPath(rule)) {
            return;
        }

        SecurityPathType securityType = resolveSecurityType(rule);

        if (rule.getMethods() == null || rule.getMethods().isEmpty()) {
            applyRule(auth, buildMatcher(null, rule), securityType, toRolesArray(rule));
            return;
        }

        rule.getMethods().forEach(method ->
                applyRule(auth, buildMatcher(method, rule), securityType, toRolesArray(rule)));
    }

    private boolean hasValidPath(SecurityConfigProperties.ProtectedPathRule rule) {
        return StringUtils.isNotBlank(rule.getPath());
    }

    private SecurityPathType resolveSecurityType(SecurityConfigProperties.ProtectedPathRule rule) {
        if (rule.getType() == null || rule.getType() == SecurityPathType.NONE) {
            return SecurityPathType.NONE;
        }

        if (rule.getType() == SecurityPathType.ROLES &&
                (rule.getAllowedRoles() == null || rule.getAllowedRoles().isEmpty())) {
            throw new IllegalArgumentException(
                    STR."Protected path rule with security type ROLES must define allowedRoles for path: \{rule.getPath()}");
        }

        return rule.getType();
    }

    private String[] toRolesArray(SecurityConfigProperties.ProtectedPathRule rule) {
        return rule.getAllowedRoles() == null
                ? new String[0]
                : rule.getAllowedRoles().toArray(new String[0]);
    }

    private RequestMatcher buildMatcher(
            org.springframework.http.HttpMethod method,
            SecurityConfigProperties.ProtectedPathRule rule) {

        if (rule.getQueryParams() == null || rule.getQueryParams().isEmpty()) {
            return method == null
                    ? AntPathRequestMatcher.antMatcher(rule.getPath())
                    : AntPathRequestMatcher.antMatcher(method, rule.getPath());
        }

        return new QueryParamPathRequestMatcher(method, rule.getPath(), rule.getQueryParams());
    }

    private void applyRule(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth,
            RequestMatcher matcher,
            SecurityPathType securityType,
            String[] allowedRoles) {

        AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizedUrl authorizedUrl = auth.requestMatchers(matcher);

        switch (securityType) {
            case NONE -> authorizedUrl.permitAll();
            case AUTHENTICATED -> authorizedUrl.authenticated();
            case ROLES -> authorizedUrl.hasAnyRole(allowedRoles);
            default -> throw new IllegalStateException(STR."Unsupported security type: \{securityType}");
        }
    }
}