package io.github.spring.middleware.client;

import io.github.spring.middleware.annotations.MiddlewareClient;
import io.github.spring.middleware.registry.model.RegistryEntry;
import io.github.spring.middleware.registry.model.RegistryMap;
import io.github.spring.middleware.registry.model.SchemaLocation;
import io.github.spring.middleware.registry.params.ResourceRegisterParameters;
import io.github.spring.middleware.registry.params.SchemaRegisterParameters;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@MiddlewareClient(timeout = 30000)
public interface RegistryClient {

    @PostMapping("/resource")
    void registerResource(@RequestBody @NotNull ResourceRegisterParameters resourceRegisterParameters);

    @PostMapping("/schema")
    void registryGraphQLSchemaLocation(@RequestBody @NotNull SchemaRegisterParameters schemaRegisterParameters);

    @DeleteMapping("/schema/{namespace}")
    void deleteGraphQLSchemaLocation(@PathVariable("namespace") String namespace);

    @GetMapping
    RegistryEntry getRegistryEntry(@RequestParam("resource") String resource);

    @GetMapping("/map")
    RegistryMap getRegistryMap();

    @GetMapping("/schema/list")
    List<SchemaLocation> getSchemaLocations();

    @GetMapping("/schema/{namespace}")
    SchemaLocation getSchemaLocation(@PathVariable("namespace") @NotNull String namespace);
}
