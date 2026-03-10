package io.github.spring.middleware.manager;

import io.github.spring.middleware.client.RegistryClient;
import io.github.spring.middleware.component.NodeInfoRetriever;
import io.github.spring.middleware.provider.ServerPortProvider;
import io.github.spring.middleware.register.graphql.GraphQLAutoRegistrar;
import io.github.spring.middleware.register.graphql.GraphQLRegisterProperties;
import io.github.spring.middleware.register.resource.ResourceAutoRegistrar;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DefaultRegistrationManager implements RegistrationManager {

    private final GraphQLAutoRegistrar graphQLAutoRegistrar;
    private final ResourceAutoRegistrar resourceAutoRegistrar;
    private final GraphQLRegisterProperties graphQLRegisterProperties;
    private final RegistryClient registryClient;
    private final ServerPortProvider serverPortProvider;
    private final NodeInfoRetriever nodeInfoRetriever;

    public DefaultRegistrationManager(@Value("${server.port:8080}") final int port,
                                      final GraphQLAutoRegistrar graphQLAutoRegistrar,
                                      final ResourceAutoRegistrar resourceAutoRegistrar,
                                      final GraphQLRegisterProperties graphQLRegisterProperties,
                                      final RegistryClient registryClient,
                                      final ServerPortProvider serverPortProvider,
                                      final NodeInfoRetriever nodeInfoRetriever) {
        this.graphQLAutoRegistrar = graphQLAutoRegistrar;
        this.resourceAutoRegistrar = resourceAutoRegistrar;
        this.graphQLRegisterProperties = graphQLRegisterProperties;
        this.registryClient = registryClient;
        this.serverPortProvider = serverPortProvider;
        this.nodeInfoRetriever = nodeInfoRetriever;
    }


    @Override
    public boolean hasSchemasToRegister() {
        return !graphQLAutoRegistrar.getSchemasToRegister().isEmpty();
    }

    @Override
    public boolean hasResourcesToRegister() {
        return !resourceAutoRegistrar.getResourcesToRegister().isEmpty();
    }

    @Override
    public void registerEverything() {
        registerResources();
        registerSchemas();
    }

    @Override
    public void registerResources() {
        resourceAutoRegistrar.registerResourcesNotRegistered();
    }

    @Override
    public void registerSchemas() {
        graphQLAutoRegistrar.reRegister();
    }

    @Override
    public void registerResourcesNotRegistered() {
        resourceAutoRegistrar.registerResourcesNotRegistered();
    }

    @Override
    public boolean isSchemaNodeRegistered() {
        try {
            if (!hasSchemasToRegister()) return true; // si no hay schemas, no aplica
            String me = STR."\{nodeInfoRetriever.getAddress()}:\{this.serverPortProvider.getPort()}";

            var schemaLocation = registryClient.getSchemaLocation(graphQLRegisterProperties.getNamespace());
            if (schemaLocation == null || schemaLocation.getSchemaLocationNodes() == null) return false;
            return schemaLocation.getSchemaLocationNodes().stream()
                    .anyMatch(n -> me.equalsIgnoreCase(n.getLocation()));
        } catch (Exception ex) {
            log.warn("Error checking if schema node is registered", ex);
            return false;
        }
    }
}

