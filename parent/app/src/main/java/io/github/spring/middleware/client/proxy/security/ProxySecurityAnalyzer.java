package io.github.spring.middleware.client.proxy.security;

import io.github.spring.middleware.annotation.security.MiddlewareApiKey;
import io.github.spring.middleware.annotation.security.MiddlewareApiKeyValue;
import io.github.spring.middleware.annotation.security.MiddlewareClientCredentials;
import io.github.spring.middleware.annotation.security.MiddlewarePassthrough;
import io.github.spring.middleware.client.proxy.security.config.SecurityApiKeyClientConfiguration;
import io.github.spring.middleware.client.proxy.security.config.SecurityClientConfiguration;
import io.github.spring.middleware.client.proxy.security.config.SecurityClientCredentialsConfiguration;
import io.github.spring.middleware.client.proxy.security.config.SecurityNoneClientConfiguration;
import io.github.spring.middleware.client.proxy.security.config.SecurityPassthroughClientConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class ProxySecurityAnalyzer implements EnvironmentAware {

    private Environment environment;

    public SecurityClientConfiguration analyze(Class<?> proxyClientInterface) {
        boolean hasClientCredentials =
                proxyClientInterface.isAnnotationPresent(MiddlewareClientCredentials.class);
        boolean hasPassthrough =
                proxyClientInterface.isAnnotationPresent(MiddlewarePassthrough.class);
        boolean hasApiKey =
                proxyClientInterface.isAnnotationPresent(MiddlewareApiKeyValue.class);

        int totalAnnotations =
                countTrue(hasClientCredentials, hasPassthrough, hasApiKey);

        if (totalAnnotations > 1) {
            throw new IllegalStateException(
                    STR."Only one middleware security annotation is allowed on proxy client interface: \{proxyClientInterface.getName()}");
        }

        if (hasClientCredentials) {
            return analyzeClientCredentials(proxyClientInterface);
        }

        if (hasPassthrough) {
            return analyzePassthrough(proxyClientInterface);
        }

        if (hasApiKey) {
            return analyzeApiKey(proxyClientInterface);
        }

        return new SecurityNoneClientConfiguration();
    }

    private SecurityClientConfiguration analyzeClientCredentials(Class<?> proxyClientInterface) {
        MiddlewareClientCredentials annotation =
                proxyClientInterface.getAnnotation(MiddlewareClientCredentials.class);

        validateRequired(environment.resolvePlaceholders(annotation.tokenUri()), "tokenUri", proxyClientInterface);
        validateRequired(environment.resolvePlaceholders(annotation.clientId()), "clientId", proxyClientInterface);
        validateRequired(environment.resolvePlaceholders(annotation.clientSecret()), "clientSecret", proxyClientInterface);

        SecurityClientCredentialsConfiguration config =
                new SecurityClientCredentialsConfiguration();
        config.setTokenUri(environment.resolvePlaceholders(annotation.tokenUri()).trim());
        config.setClientId(environment.resolvePlaceholders(annotation.clientId()).trim());
        config.setClientSecret(environment.resolvePlaceholders(annotation.clientSecret()).trim());
        return config;
    }

    private SecurityClientConfiguration analyzePassthrough(Class<?> proxyClientInterface) {
        MiddlewarePassthrough annotation =
                proxyClientInterface.getAnnotation(MiddlewarePassthrough.class);

        validateRequired(environment.resolvePlaceholders(annotation.headerName()), "headerName", proxyClientInterface);

        SecurityPassthroughClientConfiguration config =
                new SecurityPassthroughClientConfiguration();
        config.setHeaderName(environment.resolvePlaceholders(annotation.headerName()).trim());
        config.setRequired(Boolean.valueOf(environment.resolvePlaceholders(annotation.required())));

        return config;
    }

    private SecurityClientConfiguration analyzeApiKey(Class<?> proxyClientInterface) {
        MiddlewareApiKey annotation =
                proxyClientInterface.getAnnotation(MiddlewareApiKey.class);

        validateRequired(environment.resolvePlaceholders(annotation.headerName()), "headerName", proxyClientInterface);

        SecurityApiKeyClientConfiguration config =
                new SecurityApiKeyClientConfiguration();
        config.setHeaderName(environment.resolvePlaceholders(annotation.headerName()).trim());

        return config;
    }

    private void validateRequired(String value, String fieldName, Class<?> proxyClientInterface) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(
                    STR."Required security field '\{fieldName}' is missing or empty on proxy client interface: \{proxyClientInterface.getName()}");
        }
    }

    private int countTrue(boolean... values) {
        int count = 0;
        for (boolean value : values) {
            if (value) {
                count++;
            }
        }
        return count;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
