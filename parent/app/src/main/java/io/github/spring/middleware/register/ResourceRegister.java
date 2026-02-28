package io.github.spring.middleware.register;

import io.github.spring.middleware.annotations.Register;
import io.github.spring.middleware.client.RegistryClient;
import io.github.spring.middleware.registry.model.PublicServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class ResourceRegister {

    private final Set<ResourceRegisterTask> resourceRegisterTasks = Collections.synchronizedSet(new HashSet<>());
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final ResourceRegisterConfiguration resourceRegisterConfiguration;
    private final RegistryClient registryClient;
    private final PublicServer publicServer;
    private final int port;

    public ResourceRegister(@Value("${server.port}") int port, final ResourceRegisterConfiguration resourceRegisterConfiguration,
                            final RegistryClient registryClient, final PublicServer publicServer) {
        this.port = port;
        this.resourceRegisterConfiguration = resourceRegisterConfiguration;
        this.registryClient = registryClient;
        this.publicServer = publicServer;
    }

    /**
     * Registra todos los recursos anotados con @Register
     */
    public void register(Set<Class<?>> resourcesToRegister) {
        resourceRegisterTasks.clear();
        for (Class<?> clazz : resourcesToRegister) {
            if (clazz.isAnnotationPresent(Register.class)) {
                ResourceRegisterTask task = new ResourceRegisterTask(registryClient, clazz, this);
                resourceRegisterTasks.add(task);
            }
        }

        if (!resourceRegisterTasks.isEmpty()) {
            resourceRegisterTasks.forEach(task -> CompletableFuture.runAsync(task, executorService));
        }
    }

    public void remove(ResourceRegisterTask resourceRegisterTask) {
        this.resourceRegisterTasks.remove(resourceRegisterTask);
    }

    public Set<ResourceRegisterTask> getResourceRegisterTasks() {
        return resourceRegisterTasks;
    }

    public String getClusterName() {
        return resourceRegisterConfiguration.getClusterName();
    }

    public int getPort() {
        return port;
    }

    public PublicServer getPublicServer() {
        return publicServer;
    }
}



