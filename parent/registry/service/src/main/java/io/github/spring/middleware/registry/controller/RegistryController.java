package io.github.spring.middleware.registry.controller;


import io.github.spring.middleware.annotation.Register;
import io.github.spring.middleware.registry.model.RegistryEntry;
import io.github.spring.middleware.registry.model.RegistryMap;
import io.github.spring.middleware.registry.model.SchemaLocation;
import io.github.spring.middleware.registry.params.ResourceRegisterParameters;
import io.github.spring.middleware.registry.params.SchemaRegisterParameters;
import io.github.spring.middleware.registry.service.RegistryService;
import io.github.spring.middleware.registry.service.SchemaRegistryService;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@AllArgsConstructor
@Register(name = "registry")
@Slf4j
public class RegistryController {

    private final RegistryService registryService;

    private final SchemaRegistryService schemaRegistryService;

    @PostMapping("/resource")
    public void registerResource(@RequestBody @NotNull ResourceRegisterParameters resourceRegisterParameters) {
        log.debug("Received registerResource payload: {}", resourceRegisterParameters);
        registryService.registerResourceWithParams(resourceRegisterParameters);
    }

    @PostMapping("/schema")
    public void registerGraphQLSchemaLocation(@NotNull @RequestBody SchemaRegisterParameters schemaRegisterParameters) {
        schemaRegistryService.registerSchemaLocation(schemaRegisterParameters);
    }

    @DeleteMapping("/schema/{namespace}")
    public void deleteGraphQLSchemaLocation(@PathVariable("namespace") String namespace) {
        schemaRegistryService.removeSchemaLocation(namespace);
    }

    @GetMapping("/resources/{resource}")
    public RegistryEntry getRegistryEntry(@PathVariable("resource") String resource) {
        log.info("Received getRegistryEntry request for resource: {}", resource);
        return registryService.getRegistryEnry(resource);
    }

    @GetMapping("/map")
    public RegistryMap getRegistryMap() {
        return registryService.getRegistryMap();
    }

    @GetMapping("/schema/list")
    public List<SchemaLocation> getSchemaLocations() {
        return schemaRegistryService.getSchemaLocations();
    }

    @GetMapping("/schema/{namespace}")
    public SchemaLocation getSchemaLocation(@PathVariable("namespace") @NotNull String namespace) {
        return schemaRegistryService.getSchemaLocation(namespace);
    }

}
