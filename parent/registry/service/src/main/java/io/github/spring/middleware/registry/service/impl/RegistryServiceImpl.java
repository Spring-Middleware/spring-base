package io.github.spring.middleware.registry.service.impl;

import io.github.spring.middleware.registry.exceptions.PathInvalidException;
import io.github.spring.middleware.registry.model.PublicServer;
import io.github.spring.middleware.registry.model.RegistryEntry;
import io.github.spring.middleware.registry.model.RegistryMap;
import io.github.spring.middleware.registry.params.ResourceRegisterParameters;
import io.github.spring.middleware.registry.service.RegistryService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;


@Slf4j
@Service
public class RegistryServiceImpl implements RegistryService {

    private RMap<String, RegistryEntry> registryEntryMap;

    private final RedissonClient redissonClient;

    public RegistryServiceImpl(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @PostConstruct
    public void init() {
        registryEntryMap = redissonClient.getMap("RegistryEntries");
    }

    public void registerResourceWithParams(ResourceRegisterParameters resourceRegisterParameters) {

        registerResource(resourceRegisterParameters.getResourceName(),
                resourceRegisterParameters.getCluster(),
                resourceRegisterParameters.getNode(),
                resourceRegisterParameters.getPort(),
                resourceRegisterParameters.getPublicServer(),
                resourceRegisterParameters.getPath());
    }

    private void registerResource(String name, String cluster, String node, int port, PublicServer publicServer,
                                  String path) {
        if (path == null || !path.startsWith("/")) {
            throw new PathInvalidException("Path must start with '/'");
        }

        String clusterEndpoint = cluster + ":" + port + path;
        String publicEndpoint = null;
        if (publicServer != null) {
            publicEndpoint = publicServer.host() + ":" + publicServer.port() + StringUtils.defaultString(path);
        }
        RegistryEntry registryEntry = registryEntryMap.get(name);
        if (registryEntry == null) {
            registryEntry = createRegistryEntry(name, clusterEndpoint);
        }
        String nodeEndpoint = node + ":" + port + path;
        registryEntry.addNodeEndpoint(nodeEndpoint);
        registryEntry.setClusterEndpoint(clusterEndpoint);
        registryEntry.setPublicEndpoint(publicEndpoint);
        registryEntryMap.put(name, registryEntry);
        log.info("Registerd resource " + name + ":  cluster=" + clusterEndpoint + " node=" + nodeEndpoint);
    }

    private RegistryEntry createRegistryEntry(String name, String endpoint) {
        return registryEntryMap.computeIfAbsent(name, k -> new RegistryEntry(endpoint));
    }


    @Override
    public RegistryEntry getRegistryEnry(String name) {
        return this.registryEntryMap.get(name);
    }

    @Override
    public RegistryMap getRegistryMap() {
        return new RegistryMap(registryEntryMap.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue())));
    }
}
