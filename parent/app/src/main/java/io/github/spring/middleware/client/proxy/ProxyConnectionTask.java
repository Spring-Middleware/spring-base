package io.github.spring.middleware.client.proxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.spring.middleware.config.PropertyNames;
import io.github.spring.middleware.error.ErrorMessage;
import io.github.spring.middleware.error.ErrorMessageFactory;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

public class ProxyConnectionTask<T> implements Callable<T> {

    private static final Logger logger = LoggerFactory.getLogger(ProxyConnectionTask.class);

    private final WebClient webClient;
    private final String url;
    private final Method method;
    private final Object body;
    private Map<String, Object> context;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final MiddlewareClientConnectionParameters connectionParameters;
    private final ErrorMessageFactory errorMessageFactory;

    static {
        objectMapper.findAndRegisterModules();
    }

    public ProxyConnectionTask(final WebClient webClient, final String url, final Method method, final Object body,
                               final MiddlewareClientConnectionParameters connectionParameters, final ErrorMessageFactory errorMessageFactory) {
        this.webClient = webClient;
        this.url = url;
        this.method = method;
        this.body = body;
        this.connectionParameters = connectionParameters;
        this.errorMessageFactory = errorMessageFactory;
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

            processError(content, ex.getStatusCode().value(), url, method.getName());
            return null;
        } catch (Exception ex) {
            logger.warn("Error connecting to {}: {}", url, ex.getMessage(), ex);
            ProxyClientException pce = new ProxyClientException(STR."Error connecting to \{url}", ex);
            pce.addExtension("remote.url", url);
            pce.addExtension("remote.method", method.getName());
            throw pce;
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


    private void processError(String content, int statusCode, String url, String method) {
        String requestId = (String) Optional.ofNullable(context)
                .map(c -> c.get(PropertyNames.REQUEST_ID))
                .orElse(StringUtils.EMPTY);

        try {
            ErrorMessage remoteBody = objectMapper.readValue(content, ErrorMessage.class);

            // Ensure extensions is mutable
            if (remoteBody.getExtensions() == null) {
                remoteBody.setExtensions(new HashMap<>());
            } else if (!(remoteBody.getExtensions() instanceof HashMap)) {
                remoteBody.setExtensions(new HashMap<>(remoteBody.getExtensions()));
            }

            // Enriquecemos extensions SIEMPRE
            remoteBody.getExtensions().putIfAbsent("remote.url", url);
            remoteBody.getExtensions().putIfAbsent("remote.method", method);
            remoteBody.getExtensions().putIfAbsent("remote.requestId", requestId);

            throw new RemoteServerException(remoteBody, statusCode, requestId);

        } catch (Exception parseEx) {

            // No pude parsear el body remoto -> genero descriptor local (UNKNOWN)
            ErrorMessage fallback = errorMessageFactory.from(parseEx);

            // Ensure extensions is mutable
            if (fallback.getExtensions() == null) {
                fallback.setExtensions(new HashMap<>());
            } else if (!(fallback.getExtensions() instanceof java.util.HashMap)) {
                fallback.setExtensions(new HashMap<>(fallback.getExtensions()));
            }

            // Enriquecer
            fallback.getExtensions().put("remote.url", url);
            fallback.getExtensions().put("remote.method", method);
            fallback.getExtensions().put("remote.httpStatus", statusCode);
            fallback.getExtensions().put("remote.requestId", requestId);
            fallback.getExtensions().put("remote.body", safeTruncate(content, 2000));

            throw new RemoteServerException(
                    fallback,
                    // aquí también usaría rawStatus si es válido, y si no 502/500
                    statusCode > 0 ? statusCode : 500,
                    requestId
            );
        }
    }

    private String safeTruncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

}
