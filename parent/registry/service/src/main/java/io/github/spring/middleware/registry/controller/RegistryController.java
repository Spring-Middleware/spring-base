package io.github.spring.middleware.registry.controller;


import io.github.spring.middleware.annotations.Register;
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
    public void registryGraphQLSchemaLocation(@NotNull SchemaRegisterParameters schemaRegisterParameters) {
        schemaRegistryService.registerSchemaLocation(schemaRegisterParameters);
    }

    @DeleteMapping("/schema/{namespace}")
    public void deleteGraphQLSchemaLocation(@PathVariable("namespace") String namespace) {
        schemaRegistryService.removeSchemaLocation(namespace);
    }

    @GetMapping
    public RegistryEntry getRegistryEntry(@RequestParam("resource") String resource) {
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
