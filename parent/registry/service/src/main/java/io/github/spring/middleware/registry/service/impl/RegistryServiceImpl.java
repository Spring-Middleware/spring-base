package io.github.spring.middleware.registry.service.impl;

import io.github.spring.middleware.registry.exceptions.PathInvalidException;
import io.github.spring.middleware.registry.model.PublicServer;
import io.github.spring.middleware.registry.model.RegistryEntry;
import io.github.spring.middleware.registry.model.RegistryMap;
import io.github.spring.middleware.registry.params.ResourceRegisterParameters;
import io.github.spring.middleware.registry.service.RegistryService;
import io.github.spring.middleware.registry.util.EndpointUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static io.github.spring.middleware.registry.util.EndpointUtils.*;


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
                resourceRegisterParameters.getPath(),
                resourceRegisterParameters.getContextPath());
    }

    private void registerResource(String name, String cluster, String node, int port, PublicServer publicServer,
                                  String path, String contextPath) {

        contextPath = normalizeContextPath(contextPath);      // "" o "/product"
        path = normalizeResourcePath(path);                   // "" o "/graphql" ...

        String clusterEndpoint = joinUrl(STR."\{cluster}:\{port}", contextPath);          // product:8080 + /product
        clusterEndpoint = joinUrl(clusterEndpoint, path);                              // + /

        String nodeEndpoint = joinUrl(STR."\{node}:\{port}", contextPath);                 // 172.21.0.5:8080 + /product
        nodeEndpoint = joinUrl(nodeEndpoint, path);                                    // + /

        String publicEndpoint = null;
        if (publicServer != null) {
            publicEndpoint = joinUrl(STR."\{publicServer.host()}:\{publicServer.port()}", contextPath);
            publicEndpoint = joinUrl(publicEndpoint, path);
        }

        RegistryEntry registryEntry = registryEntryMap.get(name);
        if (registryEntry == null) {
            registryEntry = createRegistryEntry(name, clusterEndpoint);
        }

        registryEntry.addNodeEndpoint(nodeEndpoint);
        registryEntry.setClusterEndpoint(clusterEndpoint);
        registryEntry.setPublicEndpoint(publicEndpoint);
        registryEntry.setDateTime(LocalDateTime.now());
        registryEntryMap.put(name, registryEntry);
        log.info("Registerd resource={}  cluster={} node={}, publicEndpoint={}", name, clusterEndpoint, nodeEndpoint, publicEndpoint);
    }

    private RegistryEntry createRegistryEntry(String name, String endpoint) {
        return registryEntryMap.computeIfAbsent(name, k -> new RegistryEntry(endpoint));
    }


    @Override
    public RegistryEntry getRegistryEnry(String name) {
        final RegistryEntry registryEntry = this.registryEntryMap.get(name);
        log.debug("getRegistryEnry for name {}: {}", name, registryEntry);
        return registryEntry;
    }

    @Override
    public RegistryMap getRegistryMap() {
        return new RegistryMap(registryEntryMap.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue())));
    }

    @Override
    public boolean existsClusterHost(String hostPort) {
        if (registryEntryMap == null) return false;

        String target = EndpointUtils.extractHostPort(hostPort);

        return registryEntryMap.values().stream()
                .map(RegistryEntry::getClusterEndpoint)              // "product:8080/product"
                .map(c -> EndpointUtils.extractHostPort(c))                          // "product:8080"
                .anyMatch(target::equalsIgnoreCase);
    }


    @Override
    public void removeRegistryEntryNodeEndpoint(String clusterEndpoint, String nodeEndpoint) {
        String targetHostPort = EndpointUtils.extractHostPort(clusterEndpoint);   // "product:8080"
        String deadHostPort = EndpointUtils.extractHostPort(nodeEndpoint);      // "192.168.160.5:8080"

        Set<String> keysRemove = new HashSet<>();

        registryEntryMap.entrySet().stream()
                .filter(e -> EndpointUtils.extractHostPort(e.getValue().getClusterEndpoint())
                        .equalsIgnoreCase(targetHostPort))
                .forEach(e -> {
                    RegistryEntry re = e.getValue();

                    // borra cualquier nodeEndpoint cuyo host:port sea el muerto (da igual el "/product")
                    re.getNodeEndpoints().removeIf(ne ->
                            EndpointUtils.extractHostPort(ne).equalsIgnoreCase(deadHostPort)
                    );

                    if (re.getNodeEndpoints().isEmpty()) keysRemove.add(e.getKey());
                    else registryEntryMap.put(e.getKey(), re);
                });

        keysRemove.forEach(registryEntryMap::remove);
    }
}
