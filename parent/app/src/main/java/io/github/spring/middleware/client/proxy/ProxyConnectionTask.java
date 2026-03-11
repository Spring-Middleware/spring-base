package io.github.spring.middleware.client.proxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.spring.middleware.client.proxy.security.config.SecurityClientConfiguration;
import io.github.spring.middleware.client.proxy.security.SecurityHeaderApplier;
import io.github.spring.middleware.config.PropertyNames;
import io.github.spring.middleware.filter.Context;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class ProxyConnectionTask<T> implements Callable<T> {

    private static final Logger logger = LoggerFactory.getLogger(ProxyConnectionTask.class);

    private final WebClient webClient;
    private final String url;
    private final Method method;
    private final Object body;
    private Map<String, Object> context;
    private final MiddlewareClientConnectionParameters connectionParameters;
    private final ProxyConnectionErrorHandler errorHandler;
    private MethodMetaData methodMetaData;
    private final ObjectMapper objectMapper;
    private final SecurityHeaderApplier securityHeaderApplier;
    private final SecurityClientConfiguration securityClientConfiguration;


    public ProxyConnectionTask(final ProxyConnectionTaskParameters params) {
        this.webClient = params.getWebClient();
        this.url = params.getUrl();
        this.method = params.getMethod();
        this.body = params.getBody();
        this.connectionParameters = params.getConnectionParameters();
        this.errorHandler = params.getErrorHandler();
        this.methodMetaData = params.getMethodMetaData();
        this.objectMapper = params.getObjectMapper();
        this.securityHeaderApplier = params.getSecurityHeaderApplier();
        this.securityClientConfiguration = params.getSecurityClientConfiguration();
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    @Override
    public T call() throws Exception {
        return executeAndProcessResponse();
    }

    private <T> T executeAndProcessResponse() throws Exception {
        try {
            Mono<T> responseMono = (Mono<T>) Mono.defer(() -> {
                if (body == null) {
                    T cached = ProxyCacheSession.get(url);
                    if (cached != null) return Mono.just(cached);
                }

                // Debug: log outgoing request target and method
                if (logger.isDebugEnabled()) {
                    logger.debug("Preparing outgoing request: method={}, url={}, bodyPresent={}", method.getName(), url, body != null);
                }

                return buildRequest(methodMetaData)
                        .retrieve()
                        .bodyToMono(MethodReturnTypeResolver.getTypeReference(method))
                        .timeout(Duration.ofMillis(connectionParameters.getTimeout()))
                        .retryWhen(
                                reactor.util.retry.Retry.backoff(
                                        connectionParameters.getMaxRetries(),       // número de reintentos
                                        Duration.ofMillis(connectionParameters.getRetryBackoffMillis()) // tiempo entre reintentos
                                ).filter(t -> {
                                    if (t instanceof WebClientResponseException wex) {
                                        return !wex.getStatusCode().is4xxClientError(); // retry solo si NO es 4xx
                                    }
                                    return true; // otros errores sí (timeouts, 5xx, connect, etc.)) // opcional: no reintentar si es 4xx
                                })
                        )
                        .doOnNext(resp -> {
                            if (body == null) ProxyCacheSession.put(url, resp);
                        });
            });

            return responseMono.block(); // Bloqueamos hasta obtener la respuesta
        } catch (WebClientResponseException ex) {
            // Manejo de errores HTTP
            String content = ex.getResponseBodyAsString();
            logger.error("WebClientResponseException calling {} {} -> status: {}, body: {}",
                    method.getName(), url, ex.getStatusCode(), content);

            errorHandler.processError(content, ex.getStatusCode().value(), url, method.getName(), context);
            return null;
        } catch (Exception ex) {
            logger.warn("Error connecting to {}: {}", url, ex.getMessage(), ex);
            ProxyClientException pce = new ProxyClientException(STR."Error connecting to \{url}", ex);
            pce.addExtension("remote.url", url);
            pce.addExtension("remote.method", method.getName());
            throw pce;
        }
    }

    private WebClient.RequestHeadersSpec<?> buildRequest(final MethodMetaData methodMetaData)
            throws ProxyClientException {

        WebClient.RequestHeadersSpec<?> specHeaders;

        HttpMethod httpMethod = methodMetaData.getHttpMethod();

        if (httpMethod == HttpMethod.GET) {
            specHeaders = webClient.get().uri(URI.create(url));
        } else if (httpMethod == HttpMethod.POST) {
            specHeaders = webClient.post().uri(URI.create(url));
        } else if (httpMethod == HttpMethod.PUT) {
            specHeaders = webClient.put().uri(URI.create(url));
        } else if (httpMethod == HttpMethod.DELETE) {
            specHeaders = webClient.delete().uri(URI.create(url));
        } else if (httpMethod == HttpMethod.PATCH) {
            specHeaders = webClient.patch().uri(URI.create(url));
        } else {
            throw new ProxyClientException(
                    STR."Unsupported HTTP method for method: \{methodMetaData.getMethod().getName()}");
        }

        if (body != null && specHeaders instanceof WebClient.RequestBodySpec spec) {
            if (logger.isDebugEnabled()) {
                try {
                    String debugBody = objectMapper.writeValueAsString(body);
                    logger.debug("Outgoing body for {} -> {}", url, debugBody);
                } catch (Exception e) {
                    logger.debug("Failed to serialize outgoing body for debug: {}", e.getMessage());
                }
            }
            specHeaders = spec.contentType(MediaType.APPLICATION_JSON).bodyValue(body);
        }

        if (context != null) {
            List<String> headerNames = (List<String>) context.get(PropertyNames.HEADERS_TO_COPY);
            if (headerNames != null) {
                for (String headerName : headerNames) {
                    Object value = Context.get(headerName);
                    if (value != null && StringUtils.isNotBlank(value.toString())) {
                        specHeaders = specHeaders.header(headerName, value.toString());
                    }
                }
            }
        }

        String accept = MediaType.APPLICATION_JSON_VALUE;
        if (method.isAnnotationPresent(RequestMapping.class)) {
            String[] produces = method.getAnnotation(RequestMapping.class).produces();
            if (produces.length > 0) {
                accept = produces[0];
            }
        }

        specHeaders = specHeaders.accept(MediaType.valueOf(accept));

        Map<String, String> requestHeaders =
                context != null
                        ? (Map<String, String>) context.get(PropertyNames.REQUEST_HEADERS)
                        : null;

        specHeaders = securityHeaderApplier.applySecurityHeaders(
                securityClientConfiguration,
                requestHeaders != null ? requestHeaders : Map.of(),
                specHeaders
        );

        return specHeaders;
    }

}
