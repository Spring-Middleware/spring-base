package io.github.spring.middleware.client.proxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.spring.middleware.client.proxy.security.config.SecurityClientConfiguration;
import io.github.spring.middleware.client.proxy.security.SecurityHeaderApplier;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Method;

/**
 * Parameter holder object to pass constructor arguments to {@link ProxyConnectionTask} in a single immutable object.
 */
public final class ProxyConnectionTaskParameters {

    private final WebClient webClient;
    private final String url;
    private final Method method;
    private final Object body;
    private final MiddlewareClientConnectionParameters connectionParameters;
    private final ProxyConnectionErrorHandler errorHandler;
    private final MethodMetaData methodMetaData;
    private final ObjectMapper objectMapper;
    private final SecurityHeaderApplier securityHeaderApplier;
    private final SecurityClientConfiguration securityClientConfiguration;

    public ProxyConnectionTaskParameters(final WebClient webClient,
                                         final String url,
                                         final Method method,
                                         final Object body,
                                         final MiddlewareClientConnectionParameters connectionParameters,
                                         final ProxyConnectionErrorHandler errorHandler,
                                         final MethodMetaData methodMetaData,
                                         final ObjectMapper objectMapper,
                                         final SecurityHeaderApplier securityHeaderApplier,
                                         final SecurityClientConfiguration securityClientConfiguration) {
        this.webClient = webClient;
        this.url = url;
        this.method = method;
        this.body = body;
        this.connectionParameters = connectionParameters;
        this.errorHandler = errorHandler;
        this.methodMetaData = methodMetaData;
        this.objectMapper = objectMapper;
        this.securityHeaderApplier = securityHeaderApplier;
        this.securityClientConfiguration = securityClientConfiguration;
    }

    public WebClient getWebClient() {
        return webClient;
    }

    public String getUrl() {
        return url;
    }

    public Method getMethod() {
        return method;
    }

    public Object getBody() {
        return body;
    }

    public MiddlewareClientConnectionParameters getConnectionParameters() {
        return connectionParameters;
    }

    public ProxyConnectionErrorHandler getErrorHandler() {
        return errorHandler;
    }

    public MethodMetaData getMethodMetaData() {
        return methodMetaData;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public SecurityHeaderApplier getSecurityHeaderApplier() {
        return securityHeaderApplier;
    }

    public SecurityClientConfiguration getSecurityClientConfiguration() {
        return securityClientConfiguration;
    }

}

