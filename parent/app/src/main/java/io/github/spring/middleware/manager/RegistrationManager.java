package io.github.spring.middleware.manager;

import java.net.UnknownHostException;

public interface RegistrationManager {

    boolean hasSchemasToRegister();

    boolean hasResourcesToRegister();

    void registerEverything();     // resources + schemas + inspectors

    void registerResources();       // equivalente a resourceAutoRegistrar.registerResourcesNotRegistered()

    void registerSchemas();         // equivalente a graphQLAutoRegistrar.reRegister()

    void registerResourcesNotRegistered(); // equivalente a resourceAutoRegistrar.registerResourcesNotRegistered()

    /** true si el registry ya conoce este nodo como schema location para el namespace */
    boolean isSchemaNodeRegistered();

}
