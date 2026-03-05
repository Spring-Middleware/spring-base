package io.github.spring.middleware.client.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.spring.middleware.annotation.NoCacheSession;
import io.github.spring.middleware.client.params.MethodParamExtractor;
import io.github.spring.middleware.error.ErrorMessageFactory;
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
    private ErrorMessageFactory errorMessageFactory;
    @JsonIgnore
    private ClusterBulkheadRegistry bulkheadRegistry;
    private MiddlewareClientConnectionParameters connectionParameters;
    private Map<Method, MethodMetaData> methodMethodMetaDataMap = new HashMap<>();


    private Logger logger = LoggerFactory.getLogger(ProxyClient.class);

    public ProxyClient(Class<T> interf) {
        this.interf = interf;
        this.name = interf.getSimpleName();
    }

    public void configureHttpClient() {
        webClient = WebClientUtils.reconfigureWebClient(webClient, connectionParameters.getTimeout(), connectionParameters.getMaxConnections());
    }

    public void recreateHttpClient() {
        webClient = WebClientUtils.createWebClient(connectionParameters.getTimeout(), connectionParameters.getMaxConnections());
    }

    public void setBulkheadRegistry(final ClusterBulkheadRegistry clusterBulkheadRegistry) {
        this.bulkheadRegistry = clusterBulkheadRegistry;
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

    public void setMiddlewareClientConnectionParameters(MiddlewareClientConnectionParameters connectionParameters) {
        this.connectionParameters = connectionParameters;
    }

    public void setErrorMessageFactory(ErrorMessageFactory errorMessageFactory) {
        this.errorMessageFactory = errorMessageFactory;
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
                        Object body = extractedParams.getBody();
                        ProxyConnectionTask<T> task = new ProxyConnectionTask(webClient, url, method, body, connectionParameters, errorMessageFactory);
                        task.setContext(Context.get());

                        if (bulkheadRegistry == null) {
                            logger.warn("No bulkhead registry configured for proxy {}, executing call without bulkhead protection", getName());
                            returned = task.call();
                        } else {
                            int max = connectionParameters.getMaxConcurrentCalls();
                            var sem = bulkheadRegistry.getOrCreate(getName(), max);

                            sem.acquire();
                            try {
                                returned = task.call();
                            } finally {
                                sem.release();
                            }
                        }
                    }
                    return returned;
                } else {
                    throw new ProxyClientException(STR."ProxyClient for \{interf.getSimpleName()} is not configured");
                }

            }
        };
        return (T) proxyClass.getConstructor(new Class[]{InvocationHandler.class}).newInstance(invocationHandler);
    }

    @Override
    @JsonIgnore
    public String getClientName() {
        return getName();
    }

}
