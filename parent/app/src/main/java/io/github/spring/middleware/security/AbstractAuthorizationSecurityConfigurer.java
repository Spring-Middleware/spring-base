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
        if (configProperties.getPublicPaths() != null && !configProperties.getPublicPaths().isEmpty()) {
            auth.requestMatchers(configProperties.getPublicPaths().toArray(new String[0])).permitAll();
        }
        configProperties.getProtectedPaths().forEach(pathRule -> {
            if (pathRule.isEnabled() && pathRule.getAllowedRoles() != null && !pathRule.getAllowedRoles().isEmpty() && StringUtils.isNotBlank(pathRule.getPath())) {
                String[] allowedRoles = pathRule.getAllowedRoles().toArray(new String[0]);

                if (pathRule.getMethods().isEmpty()) {
                    auth.requestMatchers(pathRule.getPath()).hasAnyRole(allowedRoles);
                } else {
                    pathRule.getMethods().forEach(method -> auth.requestMatchers(method, pathRule.getPath()).hasAnyRole(allowedRoles));
                }
            }
        });
        auth.anyRequest().authenticated();
    }

}
