package io.github.spring.middleware.client;

import io.github.spring.middleware.annotation.MiddlewareContract;
import io.github.spring.middleware.registry.model.RegistryEntry;
import io.github.spring.middleware.registry.model.RegistryMap;
import io.github.spring.middleware.registry.model.SchemaLocation;
import io.github.spring.middleware.registry.params.ResourceRegisterParameters;
import io.github.spring.middleware.registry.params.SchemaRegisterParameters;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@MiddlewareContract(name = "registry")
public interface RegistryClient {

    @PostMapping("/resource")
    void registerResource(@RequestBody @NotNull ResourceRegisterParameters resourceRegisterParameters);

    @PostMapping("/schema")
    void registerGraphQLSchemaLocation(@RequestBody @NotNull SchemaRegisterParameters schemaRegisterParameters);

    @DeleteMapping("/schema/{namespace}")
    void deleteGraphQLSchemaLocation(@PathVariable("namespace") String namespace);

    @GetMapping("/resources/{resource}")
    RegistryEntry getRegistryEntry(@PathVariable("resource") String resource);

    @GetMapping("/map")
    RegistryMap getRegistryMap();

    @GetMapping("/schema/list")
    List<SchemaLocation> getSchemaLocations();

    @GetMapping("/schema/{namespace}")
    SchemaLocation getSchemaLocation(@PathVariable("namespace") @NotNull String namespace);

    @GetMapping("/_alive")
    Map<String, String> isAlive();
}
