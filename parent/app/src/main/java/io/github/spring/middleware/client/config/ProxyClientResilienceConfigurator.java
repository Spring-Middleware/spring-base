package io.github.spring.middleware.client.config;

import io.github.spring.middleware.client.RegistryClient;
import io.github.spring.middleware.client.proxy.ProxyClient;
import io.github.spring.middleware.registry.model.RegistryEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
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
    private final ProxyClientConfigurationProperties proxyClientConfigurationProperties;

    public ProxyClientResilienceConfigurator(RegistryClient registryClient, ProxyClientConfigurationProperties proxyClientConfigurationProperties) {
        this.proxyClientConfigurationProperties = proxyClientConfigurationProperties;
        this.registryClient = registryClient;
        this.taskExecutor = Executors.newCachedThreadPool();
    }

    /**
     * Configura todos los ProxyClient.
     */
    public void configureProxies(Set<ProxyClient<?>> proxyClients) {
        // Primero configuramos el RegistryClient
        proxyClients.stream()
                .filter(pc -> pc.getInterf().equals(RegistryClient.class))
                .forEach(pc -> {
                    pc.setRegistryEntry(new RegistryEntry(proxyClientConfigurationProperties.getRegistryEndpoint()));
                    pc.setProxyClientConfigurationProperties(proxyClientConfigurationProperties);
                    pc.configureHttpClient();
                    log.info("RegistryClient proxy configured -> {}", proxyClientConfigurationProperties.getRegistryEndpoint());
                });

        // Configuramos el resto de proxies de forma asíncrona
        configurationTasks.clear();
        for (ProxyClient<?> proxyClient : proxyClients) {
            if (!proxyClient.getInterf().equals(RegistryClient.class) && !isConfiguring(proxyClient.getInterf())) {
                ProxyClientConfigurationTask task = new ProxyClientConfigurationTask(proxyClient, registryClient, proxyClientConfigurationProperties);
                configurationTasks.add(task);
                runAsyncTask(task);
            }
        }
    }

    /**
     * Lanza una tarea en el executor de forma asíncrona.
     */
    @Async
    public void runAsyncTask(Runnable task) {
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