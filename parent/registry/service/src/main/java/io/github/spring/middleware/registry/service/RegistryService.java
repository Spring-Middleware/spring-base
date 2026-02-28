package io.github.spring.middleware.registry.service;

import io.github.spring.middleware.registry.model.RegistryEntry;
import io.github.spring.middleware.registry.model.RegistryMap;
import io.github.spring.middleware.registry.params.ResourceRegisterParameters;

public interface RegistryService {

    void registerResourceWithParams(ResourceRegisterParameters resourceRegisterParameters);

    RegistryEntry getRegistryEnry(String name);

    RegistryMap getRegistryMap();
}
