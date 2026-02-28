package io.github.spring.middleware.client.proxy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.spring.middleware.client.config.ProxyClientConfigurationProperties;
import io.github.spring.middleware.client.error.ErrorResponse;
import io.github.spring.middleware.config.PropertyNames;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

public class ProxyConnectionTask<T> implements Callable<T> {

    private static final Logger logger = LoggerFactory.getLogger(ProxyConnectionTask.class);

    private final WebClient webClient;
    private final String url;
    private final Method method;
    private final Object body;
    private final int timeoutMillis;
    private Map<String, Object> context;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final ProxyClientConfigurationProperties proxyClientConfigurationProperties;

    static {
        objectMapper.findAndRegisterModules();
    }

    public ProxyConnectionTask(WebClient webClient, String url, Method method, Object body, int timeoutMillis, final ProxyClientConfigurationProperties proxyClientConfigurationProperties) {
        this.webClient = webClient;
        this.url = url;
        this.method = method;
        this.body = body;
        this.timeoutMillis = timeoutMillis;
        this.proxyClientConfigurationProperties = proxyClientConfigurationProperties;
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

                return buildRequest()
                        .retrieve()
                        .bodyToMono(MethodReturnTypeResolver.getTypeReference(method))
                        .timeout(Duration.ofMillis(timeoutMillis))
                        .retryWhen(
                                reactor.util.retry.Retry.backoff(
                                        proxyClientConfigurationProperties.getMaxRetries(),       // número de reintentos
                                        Duration.ofMillis(proxyClientConfigurationProperties.getRetryBackoffMillis()) // tiempo entre reintentos
                                ).filter(throwable -> !(throwable instanceof WebClientResponseException.BadRequest)) // opcional: no reintentar si es 4xx
                        )
                        .doOnNext(resp -> {
                            if (body == null) ProxyCacheSession.put(url, resp);
                        });
            });

            return responseMono.block(); // Bloqueamos hasta obtener la respuesta
        } catch (WebClientResponseException ex) {
            // Manejo de errores HTTP
            String content = ex.getResponseBodyAsString();
            logger.error("WebClientResponseException calling {} {} -> status: {}, body: {}", method.getName(), url, ex.getStatusCode(), content);
            processError(content);
            return null;
        } catch (Exception ex) {
            logger.warn("Error connecting to {}: {}", url, ex.getMessage(), ex);
            throw new ProxyClientException("Error connecting to " + url, ex);
        }
    }

    private WebClient.RequestHeadersSpec<?> buildRequest() throws ProxyClientException {
        WebClient.RequestHeadersSpec<?> specHeaders;

        // Determinar método HTTP
        if (method.isAnnotationPresent(PostMapping.class)) {
            specHeaders = webClient.post().uri(URI.create(url));
        } else if (method.isAnnotationPresent(GetMapping.class)) {
            specHeaders = webClient.get().uri(URI.create(url));
        } else if (method.isAnnotationPresent(PutMapping.class)) {
            specHeaders = webClient.put().uri(URI.create(url));
        } else if (method.isAnnotationPresent(DeleteMapping.class)) {
            specHeaders = webClient.delete().uri(URI.create(url));
        } else if (method.isAnnotationPresent(PatchMapping.class)) {
            specHeaders = webClient.patch().uri(URI.create(url));
        } else {
            throw new ProxyClientException("Unsupported HTTP method for method: " + method.getName());
        }

        // Body (si aplica)
        if (body != null && specHeaders instanceof WebClient.RequestBodySpec spec) {
            // try to stringify body for debug without breaking on non-serializable objects
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

        // Headers context
        if (context != null) {
            Map<String, String> headers = (Map<String, String>) context.get(PropertyNames.HEADERS);
            if (headers != null) {
                WebClient.RequestHeadersSpec<?> finalSpecHeaders = specHeaders;
                headers.forEach((k, v) -> finalSpecHeaders.header(k, v));
            }
            String requestId = (String) context.get(PropertyNames.REQUEST_ID);
            if (StringUtils.isNotBlank(requestId)) specHeaders.header(PropertyNames.REQUEST_ID, requestId);
        }

        // Accept type
        String accept = MediaType.APPLICATION_JSON_VALUE;
        if (method.isAnnotationPresent(RequestMapping.class)) {
            String[] produces = method.getAnnotation(RequestMapping.class)
                    .produces();
            if (produces.length > 0) {
                accept = produces[0];
            }
        }
        specHeaders = specHeaders.accept(MediaType.valueOf(accept));
        return specHeaders;
    }


    private void processError(String content) throws RemoteServerException {
        try {
            ErrorResponse error = objectMapper.readValue(content, ErrorResponse.class);
            throw new RemoteServerException(String.valueOf(error), 500, (String) Optional.ofNullable(context)
                    .map(c -> c.get(PropertyNames.REQUEST_ID)).orElse(StringUtils.EMPTY));
        } catch (Exception ex) {
            throw new RemoteServerException("Error parsing error response: " + content, 500,
                    (String) Optional.ofNullable(context)
                            .map(c -> c.get(PropertyNames.REQUEST_ID)).orElse(StringUtils.EMPTY));
        }
    }

}
