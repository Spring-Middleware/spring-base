package io.github.spring.middleware.client.config;

import io.github.spring.middleware.annotation.MiddlewareContract;
import io.github.spring.middleware.annotation.MiddlewareContractConnection;
import io.github.spring.middleware.client.RegistryClient;
import io.github.spring.middleware.client.proxy.ClusterBulkheadRegistry;
import io.github.spring.middleware.client.proxy.MiddlewareClientConnectionParameters;
import io.github.spring.middleware.client.proxy.ProxyClient;
import io.github.spring.middleware.client.proxy.ProxyClientAnalyzer;
import io.github.spring.middleware.error.ErrorMessageFactory;
import io.github.spring.middleware.registry.model.RegistryEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

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
    private final ErrorMessageFactory errorMessageFactory;
    private final Environment environment;
    private final ProxyClientAnalyzer proxyClientAnalyzer;
    private final ClusterBulkheadRegistry clusterBulkheadRegistry;

    public ProxyClientResilienceConfigurator(final RegistryClient registryClient,
                                             @Value("${middleware.client.registryEndpoint}") final String registryEndpoint,
                                             final ProxyClientConfigurationTaskConfigurationProperties taskConfigProperties,
                                             final ErrorMessageFactory errorMessageFactory,
                                             final Environment environment,
                                             final ProxyClientAnalyzer proxyClientAnalyzer,
                                             final ClusterBulkheadRegistry clusterBulkheadRegistry) {
        this.registryEndpoint = registryEndpoint;
        this.registryClient = registryClient;
        this.taskConfigProperties = taskConfigProperties;
        this.errorMessageFactory = errorMessageFactory;
        this.taskExecutor = Executors.newFixedThreadPool(taskConfigProperties.getThreadPoolSize());
        this.environment = environment;
        this.proxyClientAnalyzer = proxyClientAnalyzer;
        this.clusterBulkheadRegistry = clusterBulkheadRegistry;
    }

    /**
     * Configura todos los ProxyClient.
     */
    public void configureProxies(Set<ProxyClient<?>> proxyClients) {
        // Primero configuramos el RegistryClient
        proxyClients.stream()
                .filter(pc -> pc.getInterf().equals(RegistryClient.class))
                .forEach(pc -> {
                    MiddlewareClientConnectionParameters clientConnectionParameters = createConnectionParameters(pc);
                    pc.setRegistryEntry(new RegistryEntry(registryEndpoint));
                    pc.setMiddlewareClientConnectionParameters(clientConnectionParameters);
                    pc.setBulkheadRegistry(clusterBulkheadRegistry);
                    pc.setErrorMessageFactory(errorMessageFactory);
                    pc.setMethodMethodMetaDataMap(proxyClientAnalyzer.analize(pc.getInterf()));
                    pc.configureHttpClient();
                    log.info("RegistryClient proxy configured -> {}", registryEndpoint);
                });

        // Configuramos el resto de proxies de forma asíncrona
        configurationTasks.clear();
        for (ProxyClient<?> proxyClient : proxyClients) {
            if (!proxyClient.getInterf().equals(RegistryClient.class) && !isConfiguring(proxyClient.getInterf())) {
                MiddlewareClientConnectionParameters clientConnectionParameters = createConnectionParameters(proxyClient);
                ProxyClientConfigurationTask task = new ProxyClientConfigurationTask(proxyClient,
                        registryClient,
                        clientConnectionParameters,
                        taskConfigProperties,
                        errorMessageFactory,
                        proxyClientAnalyzer,
                        clusterBulkheadRegistry);
                configurationTasks.add(task);
                runAsyncTask(task);
            }
        }
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


    private void runAsyncTask(Runnable task) {
        taskExecutor.execute(task);
    }

    public boolean isConfigured(Class<?> clazz) {
        return configurationTasks.stream()
                .filter(task -> task.getProxyClient().getInterf().equals(clazz))
                .findFirst()
                .map(task -> task.getProxyClient().getRegistryEntry() != null)
                .orElse(Boolean.FALSE);
    }

    public boolean isConfiguring(Class<?> clazz) {
        return configurationTasks.stream()
                .anyMatch(task -> task.getProxyClient().getInterf().equals(clazz));
    }

    public void stopAll() {
        configurationTasks.forEach(ProxyClientConfigurationTask::stop);
    }
}