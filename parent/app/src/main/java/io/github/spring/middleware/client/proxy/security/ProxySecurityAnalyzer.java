package io.github.spring.middleware.client.proxy.security;

import io.github.spring.middleware.annotation.MiddlewareContract;
import io.github.spring.middleware.annotation.security.MiddlewareApiKey;
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

import java.util.Arrays;
import java.util.stream.Collectors;

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
                proxyClientInterface.isAnnotationPresent(MiddlewareApiKey.class);

        MiddlewareContract middlewareContract = proxyClientInterface.getAnnotation(MiddlewareContract.class);
        String securityClientTypePlain = environment.resolvePlaceholders(middlewareContract.security());

        if (securityClientTypePlain == null || securityClientTypePlain.startsWith("${") && securityClientTypePlain.endsWith("}")) {
            throw new IllegalStateException(
                    STR."Security client type is not defined in @MiddlewareContract annotation on proxy client interface: \{proxyClientInterface.getName()}");
        }

        SecurityClientType securityClientType = null;
        try {
            securityClientType = SecurityClientType.valueOf(securityClientTypePlain.trim());
        } catch (Exception ex) {
            String expectedValues = Arrays.stream(SecurityClientType.values())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));

            throw new IllegalStateException(
                    STR."Invalid security client type '\{securityClientTypePlain}' defined in @MiddlewareContract annotation on proxy client interface: \{proxyClientInterface.getName()}. Expected values are: [\{expectedValues}]"
            );
        }

        if (hasClientCredentials && securityClientType == SecurityClientType.OAUTH2_CLIENT_CREDENTIALS) {
            return analyzeClientCredentials(proxyClientInterface);
        }

        if (hasPassthrough && securityClientType == SecurityClientType.PASSTHROUGH) {
            return analyzePassthrough(proxyClientInterface);
        }

        if (hasApiKey && securityClientType == SecurityClientType.API_KEY) {
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
        config.setApiKeyValue(environment.resolvePlaceholders(annotation.value()).trim());
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
