package io.github.spring.middleware.manager;

import io.github.spring.middleware.annotation.Register;
import io.github.spring.middleware.client.RegistryClient;
import io.github.spring.middleware.client.config.RegistryType;
import io.github.spring.middleware.register.graphql.GraphQLAutoRegistrar;
import io.github.spring.middleware.register.graphql.GraphQLRegisterProperties;
import io.github.spring.middleware.register.resource.ResourceAutoRegistrar;
import io.github.spring.middleware.utils.EndpointUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.UnknownHostException;
import java.util.Set;

@Slf4j
@Component
public class DefaultRegistrationManager implements RegistrationManager {

    private final GraphQLAutoRegistrar graphQLAutoRegistrar;
    private final ResourceAutoRegistrar resourceAutoRegistrar;
    private final GraphQLRegisterProperties graphQLRegisterProperties;
    private final RegistryClient registryClient;
    private final RegistryType registryType;

    public DefaultRegistrationManager(@Value("${server.port:8080}") final int port,
                                      final GraphQLAutoRegistrar graphQLAutoRegistrar,
                                      final ResourceAutoRegistrar resourceAutoRegistrar,
                                      final GraphQLRegisterProperties graphQLRegisterProperties,
                                      final RegistryClient registryClient) {
        this.graphQLAutoRegistrar = graphQLAutoRegistrar;
        this.resourceAutoRegistrar = resourceAutoRegistrar;
        this.graphQLRegisterProperties = graphQLRegisterProperties;
        this.registryClient = registryClient;
        this.registryType = RegistryType.resolve(registryClient);
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
    public boolean isSchemaNodeRegistered() {
        if (registryType == RegistryType.NO_OP) {
            log.debug("Registry type is NO_OP, assuming schema node is registered");
            return true;
        }
        try {
            if (!hasSchemasToRegister()) return true; // si no hay schemas, no aplica
            String me = graphQLAutoRegistrar.getSchemaLocationNodeName();

            var schemaLocation = registryClient.getSchemaLocation(graphQLRegisterProperties.getNamespace());
            if (schemaLocation == null || schemaLocation.getSchemaLocationNodes() == null) return false;
            return schemaLocation.getSchemaLocationNodes().stream()
                    .anyMatch(n -> me.equalsIgnoreCase(n.getLocation()));
        } catch (Exception ex) {
            log.warn("Error checking if schema node is registered", ex);
            return false;
        }
    }

    @Override
    public boolean isEndpointRegistered(Register register) {
        if (registryType == RegistryType.NO_OP) {
            log.debug("Registry type is NO_OP, assuming endpoint for resource {} is registered", register.name());
            return true;
        }

        var registryEntry = registryClient.getRegistryEntry(register.name());
        if (registryEntry == null || registryEntry.getNodeEndpoints() == null) return false;
        String me;
        try {
            me = EndpointUtils.normalizeEndpoint(resourceAutoRegistrar.getNodeEndpointName(register.path()));
        } catch (UnknownHostException e) {
            log.warn("Error retrieving local host address", e);
            return false;
        }

        boolean endpointRegistered = registryEntry.getNodeEndpoints().stream()
                .anyMatch(e -> me.equalsIgnoreCase(e.getNodeEndpoint()));
        if (!endpointRegistered) {
            log.warn("Endpoint for resource {} is NOT registered", register.name());
        } else {
            log.debug("Endpoint for resource {} is registered", register.name());
        }
        return endpointRegistered;
    }

    @Override
    public Set<Class<?>> getResourcesToRegister() {
        return resourceAutoRegistrar.getResourcesToRegister();
    }

    @Override
    public void registerResources(Set<Class<?>> resourcesClasses) {
        resourceAutoRegistrar.registerResources(resourcesClasses);
    }
}





