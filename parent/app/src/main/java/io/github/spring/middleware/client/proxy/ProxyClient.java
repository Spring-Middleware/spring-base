package io.github.spring.middleware.client.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.spring.middleware.annotation.NoCacheSession;
import io.github.spring.middleware.filter.Context;
import io.github.spring.middleware.registry.model.RegistryEntry;
import io.github.spring.middleware.util.WebClientUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProxyClient<T> implements ClientConfigurable {

    @JsonIgnore
    private Class<T> interf;
    private RegistryEntry registryEntry;
    @JsonIgnore
    private WebClient webClient = null;
    private String name;
    private ProxyConnectionErrorHandler errorHandler;
    @JsonIgnore
    private ClusterBulkheadRegistry bulkheadRegistry;
    private MiddlewareClientConfigParameters clientConfigParameters;
    private Map<Method, MethodMetaData> methodMethodMetaDataMap = new HashMap<>();
    private ClusterCircuitBreakerRegistry circuitBreakerRegistry;
    private ObjectMapper objectMapper;


    private Logger logger = LoggerFactory.getLogger(ProxyClient.class);

    public ProxyClient(Class<T> interf) {
        this.interf = interf;
        this.name = interf.getSimpleName();
    }

    public void configureHttpClient() {
        var connectionParameters = clientConfigParameters != null ? clientConfigParameters.getConnectionParameters() : MiddlewareClientConnectionParameters.defaultParameters();
        webClient = WebClientUtils.reconfigureWebClient(webClient, connectionParameters.getTimeout(), connectionParameters.getMaxConnections());
    }

    public void recreateHttpClient() {
        var connectionParameters = clientConfigParameters != null ? clientConfigParameters.getConnectionParameters() : MiddlewareClientConnectionParameters.defaultParameters();
        webClient = WebClientUtils.createWebClient(connectionParameters.getTimeout(), connectionParameters.getMaxConnections());
    }

    public void setBulkheadRegistry(final ClusterBulkheadRegistry clusterBulkheadRegistry) {
        this.bulkheadRegistry = clusterBulkheadRegistry;
    }

    public void setCircuitBreakerRegistry(final ClusterCircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    public void setObjectMapper(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @JsonProperty("name")
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("registryEntry")
    public RegistryEntry getRegistryEntry() {
        return this.registryEntry;
    }

    public void setMiddlewareClientConfigParameters(MiddlewareClientConfigParameters clientConfigParameters) {
        this.clientConfigParameters = clientConfigParameters;
    }

    public void setErrorHandler(ProxyConnectionErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }


    public void setMethodMethodMetaDataMap(Map<Method, MethodMetaData> methodMethodMetaDataMap) {
        this.methodMethodMetaDataMap = methodMethodMetaDataMap;
    }

    public Class<T> getInterf() {
        return interf;
    }

    public void setRegistryEntry(RegistryEntry registryEntry) {
        this.registryEntry = registryEntry;
    }

    public T wrappedInstance() throws Exception {

        Class<?> proxyClass = Proxy.getProxyClass(interf.getClassLoader(), interf);
        InvocationHandler invocationHandler = new InvocationHandler() {

            @Override
            public T invoke(Object proxy, Method method, Object[] args) throws Throwable {

                if (method.getDeclaringClass() == Object.class) {
                    return (T) switch (method.getName()) {
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        case "toString" -> "ProxyClient(" + interf.getSimpleName() + ")";
                        default -> method.invoke(this, args);
                    };
                }

                final MethodMetaData methodMetaData = methodMethodMetaDataMap.get(method);
                MethodMetaData.ExtractedParams extractedParams = methodMetaData.extractedParams(args);
                String queryPath = ResourceMetadaURLResolver.resolvePath(extractedParams.getPath(), extractedParams.getPathVariables(), extractedParams.getRequestParams());
                if (registryEntry != null && registryEntry.getClusterEndpoint() != null) {
                    String url = UrlJoiner.join(registryEntry.getClusterEndpoint(), queryPath);

                    // Ensure WebClient is initialized
                    if (webClient == null) {
                        logger.debug("WebClient is null for proxy {}, recreating client", getName());
                        recreateHttpClient();
                    }

                    // Debug logging to help diagnose calls to self
                    if (logger.isDebugEnabled()) {
                        logger.debug("Invoking remote resource: client={}, method={}, clusterEndpoint={}, queryPath={}, url={}", getName(), method.getName(), registryEntry.getClusterEndpoint(), queryPath, url);
                        Object bodyDbg = extractedParams.getBody();
                        try {
                            logger.debug("Outgoing body (toString): {}", bodyDbg != null ? bodyDbg.toString() : "<null>");
                        } catch (Exception e) {
                            logger.debug("Unable toString outgoing body: {}", e.getMessage());
                        }
                    }

                    T returned = null;

                    boolean isCacheable = extractedParams.getBody() == null
                            && !method.isAnnotationPresent(NoCacheSession.class)
                            && !method.isAnnotationPresent(DeleteMapping.class);

                    if (isCacheable) {
                        returned = ProxyCacheSession.get(url);
                    }

                    if (returned == null) {
                        var connectionParameters = clientConfigParameters != null ? clientConfigParameters.getConnectionParameters() : MiddlewareClientConnectionParameters.defaultParameters();
                        Object body = extractedParams.getBody();

                        ProxyConnectionTaskParameters params = new ProxyConnectionTaskParameters(webClient, url, method, body, connectionParameters, errorHandler, methodMetaData, objectMapper);
                        ProxyConnectionTask<T> task = new ProxyConnectionTask<>(params);
                        task.setContext(Context.get());

                        MiddlewareCircuitBreakerParameters circuitBreakerParameters =
                                methodMetaData.getCircuitBreakerParameters() != null
                                        ? methodMetaData.getCircuitBreakerParameters()
                                        : clientConfigParameters != null ? clientConfigParameters.getCircuitBreakerParameters() : null;

                        if (circuitBreakerParameters == null || !circuitBreakerParameters.isEnanbled()) {
                            logger.warn("Proxy client {} method {} is not configured with circuit breaker parameters, executing call without circuit breaker protection", getName(), method.getName());
                            returned = executeCall(connectionParameters.getMaxConcurrentCalls(), task);
                        } else {
                            var circuitBreaker = circuitBreakerRegistry.getOrCreate(methodMetaData.getCircuitBreakerKey(registryEntry.getName()), circuitBreakerParameters);
                            returned = circuitBreaker.executeCallable(() ->
                                    executeCall(connectionParameters.getMaxConcurrentCalls(), task)
                            );
                        }
                    }
                    return returned;
                } else {
                    ProxyClientUnavailableException ex =
                            new ProxyClientUnavailableException(STR."ProxyClient for \{interf.getSimpleName()} is not configured");
                    ex.addExtension("client.name", getName());
                    ex.addExtension("client.interface", interf.getName());
                    ex.addExtension("registry.entry", registryEntry != null ? registryEntry.getName() : null);
                    throw ex;
                }

            }
        };
        return (T) proxyClass.getConstructor(new Class[]{InvocationHandler.class}).newInstance(invocationHandler);
    }


    private T executeCall(int maxConcurrentCalls, ProxyConnectionTask<T> task) throws Exception {
        if (bulkheadRegistry == null) {
            logger.warn("No bulkhead registry configured for proxy {}, executing call without bulkhead protection", getName());
            return task.call();
        } else {
            var sem = bulkheadRegistry.getOrCreate(getName(), maxConcurrentCalls);
            sem.acquire();
            try {
                return task.call();
            } finally {
                sem.release();
            }
        }
    }


    @Override
    @JsonIgnore
    public String getClientName() {
        return getName();
    }

}
