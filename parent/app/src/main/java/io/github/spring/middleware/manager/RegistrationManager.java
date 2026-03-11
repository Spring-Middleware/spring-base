package io.github.spring.middleware.manager;

import io.github.spring.middleware.annotation.Register;

import java.util.Set;

public interface RegistrationManager {

    boolean hasSchemasToRegister();

    boolean hasResourcesToRegister();

    void registerEverything();     // resources + schemas + inspectors

    void registerResources();       // equivalente a resourceAutoRegistrar.registerResourcesNotRegistered()

    void registerSchemas();         // equivalente a graphQLAutoRegistrar.reRegister()

    /** true si el registry ya conoce este nodo como schema location para el namespace */
    boolean isSchemaNodeRegistered();

    boolean isEndpointRegistered(Register register);

    Set<Class<?>> getResourcesToRegister();

    void registerResources(Set<Class<?>> resourcesClasses);

}
