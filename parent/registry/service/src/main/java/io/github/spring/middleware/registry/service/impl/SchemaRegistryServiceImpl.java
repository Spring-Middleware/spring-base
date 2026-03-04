package io.github.spring.middleware.registry.service.impl;

import io.github.spring.middleware.registry.model.SchemaLocation;
import io.github.spring.middleware.registry.model.SchemaLocationNode;
import io.github.spring.middleware.registry.params.SchemaRegisterParameters;
import io.github.spring.middleware.registry.service.SchemaRegistryService;
import jakarta.annotation.PostConstruct;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;

/**
 * Stores GraphQL schema locations in Redis (Redisson).
 *
 * Key:   namespace
 * Value: SchemaLocation (includes node locations list)
 */
@Service
public class SchemaRegistryServiceImpl implements SchemaRegistryService {

    private RMap<String, SchemaLocation> schemaLocationsMap;
    private final RedissonClient redissonClient;

    public SchemaRegistryServiceImpl(RedissonClient redissonClient) {
        Objects.requireNonNull(redissonClient, "redissonClient");
        this.redissonClient = redissonClient;
    }

    @PostConstruct
    public void init() {
        this.schemaLocationsMap = redissonClient.getMap("SchemaLocations");
    }

    @Override
    public void registerSchemaLocation(SchemaRegisterParameters params) {
        Objects.requireNonNull(params, "schemaRegisterParameters");
        if (params.getNamespace() == null || params.getNamespace().isBlank()) {
            throw new IllegalArgumentException("namespace is required and cannot be blank parameters");
        }

        final String namespace = params.getNamespace().trim();

        // Atomic update with RMap.compute (safe under concurrency)
        schemaLocationsMap.compute(namespace, (k, current) -> {
            SchemaLocation schemaLocation = current != null ? current : new SchemaLocation();

            // Mandatory identifiers
            schemaLocation.setNamespace(namespace);

            // Location of the schema (cluster/service endpoint)
            schemaLocation.setLocation(params.getLocation());
            schemaLocation.setPathApi(params.getPathApi());

            // Ensure node list exists
            if (schemaLocation.getSchemaLocationNodes() == null) {
                schemaLocation.setSchemaLocationNodes(new HashSet<>());
            }

            // Node location (pod/instance), ensure uniqueness
            String nodeLocation = params.getNodeLocation();
            if (nodeLocation != null && !nodeLocation.isBlank()) {
                boolean exists = schemaLocation.getSchemaLocationNodes().stream()
                        .anyMatch(n -> nodeLocation.equalsIgnoreCase(n.getLocation()));

                if (!exists) {
                    SchemaLocationNode node = new SchemaLocationNode();
                    node.setId(UUID.randomUUID());
                    node.setNamespace(namespace);
                    node.setLocation(nodeLocation);
                    node.setLastAliveCheckDate(new Timestamp(System.currentTimeMillis()));
                    schemaLocation.getSchemaLocationNodes().add(node);
                } else {
                    // Refresh last alive for existing node
                    Timestamp now = new Timestamp(System.currentTimeMillis());
                    schemaLocation.getSchemaLocationNodes().stream()
                            .filter(n -> nodeLocation.equalsIgnoreCase(n.getLocation()))
                            .forEach(n -> n.setLastAliveCheckDate(now));
                }
            }

            return schemaLocation;
        });
    }

    @Override
    public List<SchemaLocation> getSchemaLocations() {
        // Copy to avoid exposing live Redisson collection
        return new ArrayList<>(schemaLocationsMap.values());
    }

    @Override
    public void removeSchemaLocation(String namespace) {
        if (namespace == null || namespace.isBlank()) return;
        schemaLocationsMap.remove(namespace.trim());
    }

    @Override
    public void removeSchemaLocationNode(String namespace, String locationNode) {
        if (namespace == null || namespace.isBlank()) return;
        if (locationNode == null || locationNode.isBlank()) return;

        final String ns = namespace.trim();
        final String nodeLoc = locationNode.trim();

        schemaLocationsMap.computeIfPresent(ns, (k, schemaLocation) -> {
            if (schemaLocation.getSchemaLocationNodes() != null) {
                schemaLocation.getSchemaLocationNodes()
                        .removeIf(n -> nodeLoc.equalsIgnoreCase(n.getLocation()));
            }
            return schemaLocation;
        });
    }

    @Override
    public void refreshNodeLastAliveCheckDate(SchemaLocation schemaLocation, String location) {

        schemaLocation.getSchemaLocationNodes().stream().filter(sln -> sln.getLocation().equals(location))
                .forEach(sln -> sln.setLastAliveCheckDate(new Timestamp(System.currentTimeMillis())));

        schemaLocationsMap.put(schemaLocation.getNamespace(), schemaLocation);
    }

    @Override
    public SchemaLocation getSchemaLocation(String namespace) {
        if (namespace == null || namespace.isBlank()) return null;
        return schemaLocationsMap.get(namespace.trim());
    }
}