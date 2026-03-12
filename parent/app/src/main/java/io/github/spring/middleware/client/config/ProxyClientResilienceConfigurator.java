package io.github.spring.middleware.client.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.spring.middleware.annotation.MiddlewareCircuitBreaker;
import io.github.spring.middleware.annotation.MiddlewareContract;
import io.github.spring.middleware.annotation.MiddlewareContractConnection;
import io.github.spring.middleware.client.RegistryClient;
import io.github.spring.middleware.client.proxy.ClusterBulkheadRegistry;
import io.github.spring.middleware.client.proxy.ClusterCircuitBreakerRegistry;
import io.github.spring.middleware.client.proxy.MiddlewareCircuitBreakerParameters;
import io.github.spring.middleware.client.proxy.MiddlewareClientConfigParameters;
import io.github.spring.middleware.client.proxy.MiddlewareClientConnectionParameters;
import io.github.spring.middleware.client.proxy.ProxyClient;
import io.github.spring.middleware.client.proxy.ProxyClientAnalyzer;
import io.github.spring.middleware.client.proxy.ProxyClientRegistry;
import io.github.spring.middleware.client.proxy.ProxyConnectionErrorHandler;
import io.github.spring.middleware.client.proxy.security.ProxySecurityAnalyzer;
import io.github.spring.middleware.client.proxy.security.SecurityManagerApplier;
import io.github.spring.middleware.registry.model.RegistryEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class ProxyClientResilienceConfigurator {

    private final Set<ProxyClientConfigurationTask> configurationTasks = new HashSet<>();
    private final RegistryClient registryClient;
    private final Executor taskExecutor;
    private final String registryEndpoint;
    private final ProxyClientConfigurationTaskConfigurationProperties taskConfigProperties;
    private final ProxyConnectionErrorHandler errorHandler;
    private final Environment environment;
    private final ProxyClientAnalyzer proxyClientAnalyzer;
    private final ProxySecurityAnalyzer proxySecurityAnalyzer;
    private final SecurityManagerApplier securityManagerApplier;
    private final ClusterBulkheadRegistry clusterBulkheadRegistry;
    private final ClusterCircuitBreakerRegistry clusterCircuitBreakerRegistry;
    private final ObjectMapper objectMapper;

    public ProxyClientResilienceConfigurator(final RegistryClient registryClient,
                                             @Value("${middleware.client.registry-endpoint}") final String registryEndpoint,
                                             final ProxyClientConfigurationTaskConfigurationProperties taskConfigProperties,
                                             final ProxyConnectionErrorHandler proxyConnectionErrorHandler,
                                             final Environment environment,
                                             final ProxyClientAnalyzer proxyClientAnalyzer,
                                             final ProxySecurityAnalyzer proxySecurityAnalyzer,
                                             final ClusterBulkheadRegistry clusterBulkheadRegistry,
                                             final ClusterCircuitBreakerRegistry clusterCircuitBreakerRegistry,
                                             final ObjectMapper objectMapper,
                                             final SecurityManagerApplier securityManagerApplier) {
        this.registryEndpoint = registryEndpoint;
        this.registryClient = registryClient;
        this.taskConfigProperties = taskConfigProperties;
        this.errorHandler = proxyConnectionErrorHandler;
        this.taskExecutor = Executors.newFixedThreadPool(taskConfigProperties.getThreadPoolSize());
        this.environment = environment;
        this.proxyClientAnalyzer = proxyClientAnalyzer;
        this.proxySecurityAnalyzer = proxySecurityAnalyzer;
        this.clusterBulkheadRegistry = clusterBulkheadRegistry;
        this.clusterCircuitBreakerRegistry = clusterCircuitBreakerRegistry;
        this.objectMapper = objectMapper;
        this.securityManagerApplier = securityManagerApplier;
    }

    /**
     * Configura todos los ProxyClient.
     */
    public void configureProxies(Set<ProxyClient<?>> proxyClients) {
        // Primero configuramos el RegistryClient
        proxyClients.stream()
                .filter(pc -> pc.getInterf().equals(RegistryClient.class) && pc.getRegistryEntry() == null)
                .forEach(pc -> {
                    MiddlewareClientConfigParameters clientConfigParameters = createClientConfigParameters(pc);
                    pc.setRegistryEntry(new RegistryEntry(registryEndpoint,"registry"));
                    pc.setMiddlewareClientConfigParameters(clientConfigParameters);
                    pc.setBulkheadRegistry(clusterBulkheadRegistry);
                    pc.setCircuitBreakerRegistry(clusterCircuitBreakerRegistry);
                    pc.setSecurityManagerApplier(securityManagerApplier);
                    pc.setObjectMapper(objectMapper);
                    pc.setErrorHandler(errorHandler);
                    pc.setMethodMethodMetaDataMap(proxyClientAnalyzer.analyze(pc.getInterf()));
                    pc.configureHttpClient();
                    log.info("RegistryClient proxy configured -> {}", registryEndpoint);
                });

        // Configuramos el resto de proxies de forma asíncrona
        configurationTasks.clear();
        for (ProxyClient<?> proxyClient : proxyClients) {
            configureProxy(proxyClient);
        }
    }

    public void configureProxy(ProxyClient<?> proxyClient) {
        if (canConfigureProxy(proxyClient)) {
            MiddlewareClientConfigParameters clientConfigParameters = createClientConfigParameters(proxyClient);
            ProxyClientConfigurationTask task = new ProxyClientConfigurationTask(proxyClient,
                    registryClient,
                    clientConfigParameters,
                    taskConfigProperties,
                    errorHandler,
                    proxyClientAnalyzer,
                    clusterBulkheadRegistry,
                    clusterCircuitBreakerRegistry,
                    objectMapper,
                    securityManagerApplier);
            configurationTasks.add(task);
            runAsyncTask(task);
        }
    }

    private boolean canConfigureProxy(ProxyClient<?> proxyClient) {
        MiddlewareContract middlewareContract = proxyClient.getInterf().getAnnotation(MiddlewareContract.class);
        boolean enabled = Boolean.valueOf(environment.resolvePlaceholders(middlewareContract.enabled()));
        return !proxyClient.getInterf().equals(RegistryClient.class) && !isConfiguring(proxyClient.getInterf())
                && enabled && (proxyClient.getRegistryEntry() == null || proxyClient.getRegistryEntry().getClusterEndpoint() == null);
    }


    private MiddlewareClientConfigParameters createClientConfigParameters(ProxyClient<?> proxyClient) {
        MiddlewareClientConfigParameters clientConfigParameters = new MiddlewareClientConfigParameters();
        clientConfigParameters.setConnectionParameters(createConnectionParameters(proxyClient));
        clientConfigParameters.setCircuitBreakerParameters(clientCircuitBreakerParameters(proxyClient));
        clientConfigParameters.setSecurityClientConfiguration(proxySecurityAnalyzer.analyze(proxyClient.getInterf()));
        return clientConfigParameters;
    }


    private MiddlewareCircuitBreakerParameters clientCircuitBreakerParameters(ProxyClient<?> proxyClient) {
        MiddlewareCircuitBreakerParameters circuitBreakerParameters = new MiddlewareCircuitBreakerParameters();
        MiddlewareContract middlewareContract = proxyClient.getInterf().getAnnotation(MiddlewareContract.class);
        MiddlewareCircuitBreaker contractCircuitBreaker = middlewareContract.circuitBreaker();
        circuitBreakerParameters.setEnanbled(Boolean.valueOf(environment.resolvePlaceholders(contractCircuitBreaker.enabled())));
        circuitBreakerParameters.setFailureRateThreshold(Float.valueOf(environment.resolvePlaceholders(contractCircuitBreaker.failureRateThreshold())));
        circuitBreakerParameters.setMinimumNumberOfCalls(Integer.valueOf(environment.resolvePlaceholders(contractCircuitBreaker.minimumNumberOfCalls())));
        circuitBreakerParameters.setSlidingWindowSize(Integer.valueOf(environment.resolvePlaceholders(contractCircuitBreaker.slidingWindowSize())));
        circuitBreakerParameters.setWaitDurationInOpenStateMs(Long.valueOf(environment.resolvePlaceholders(contractCircuitBreaker.waitDurationInOpenStateMs())));
        circuitBreakerParameters.setPermittedNumberOfCallsInHalfOpenState(Integer.valueOf(environment.resolvePlaceholders(contractCircuitBreaker.permittedNumberOfCallsInHalfOpenState())));
        Arrays.stream(contractCircuitBreaker.statusShouldOpenBreaker()).forEach(expresion -> {
            circuitBreakerParameters.getOpenCircuitBreakerStatusExpressions().add(environment.resolvePlaceholders(expresion));
        });
        Arrays.stream(contractCircuitBreaker.statusShouldIgnoreBreaker()).forEach(expresion -> {
            circuitBreakerParameters.getIgnoreCircuitBreakerStatusExpressions().add(environment.resolvePlaceholders(expresion));
        });
        return circuitBreakerParameters;
    }


    private MiddlewareClientConnectionParameters createConnectionParameters(ProxyClient<?> proxyClient) {
        MiddlewareContract middlewareContract = proxyClient.getInterf().getAnnotation(MiddlewareContract.class);
        MiddlewareContractConnection contractConnection = middlewareContract.connection();
        MiddlewareClientConnectionParameters parameters = new MiddlewareClientConnectionParameters();
        parameters.setMaxRetries(Integer.valueOf(environment.resolvePlaceholders(contractConnection.maxRetries())));
        parameters.setTimeout(Integer.valueOf(environment.resolvePlaceholders(contractConnection.timeout())));
        parameters.setRetryBackoffMillis(Integer.valueOf(environment.resolvePlaceholders(contractConnection.retryBackoffMillis())));
        parameters.setMaxConnections(Integer.valueOf(environment.resolvePlaceholders(contractConnection.maxConnections())));
        parameters.setMaxConcurrentCalls(Integer.valueOf(environment.resolvePlaceholders(contractConnection.maxConcurrentCalls())));
        return parameters;
    }

    public void desconfigureClient(String clientName) {
        ProxyClientRegistry.getAll().stream().filter(p -> clientName.equals(p.getRegistryEntry().getName()) && p.getRegistryEntry().getClusterEndpoint() != null)
                .forEach(proxyClient -> {
                    proxyClient.getRegistryEntry().setClusterEndpoint(null);
                    log.info(STR."Desconfigured proxy client for \{proxyClient.getInterf().getSimpleName()}");
                });
    }


    private void runAsyncTask(Runnable task) {
        taskExecutor.execute(() -> {
            try {
                task.run();
            } finally {
                configurationTasks.remove(task);
            }
        });
    }

    public boolean isConfiguring(Class<?> clazz) {
        return configurationTasks.stream()
                .anyMatch(task -> task.getProxyClient().getInterf().equals(clazz));
    }

    public void stopAll() {
        configurationTasks.forEach(ProxyClientConfigurationTask::stop);
    }
}