package io.github.spring.middleware.registry.service.impl;

import io.github.spring.middleware.jms.client.ProxyClientEvent;
import io.github.spring.middleware.jms.client.ProxyClientEventType;
import io.github.spring.middleware.registry.jms.ProxyClientEventResourceProducer;
import io.github.spring.middleware.registry.model.NodeEndpoint;
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
import java.util.UUID;
import java.util.stream.Collectors;

import static io.github.spring.middleware.registry.util.EndpointUtils.joinUrl;
import static io.github.spring.middleware.registry.util.EndpointUtils.normalizeContextPath;
import static io.github.spring.middleware.registry.util.EndpointUtils.normalizeResourcePath;


@Slf4j
@Service
public class RegistryServiceImpl implements RegistryService {

    private RMap<String, RegistryEntry> registryEntryMap;

    private final RedissonClient redissonClient;
    private final ProxyClientEventResourceProducer proxyClientEventResourceProducer;

    public RegistryServiceImpl(RedissonClient redissonClient, ProxyClientEventResourceProducer proxyClientEventResourceProducer) {
        this.redissonClient = redissonClient;
        this.proxyClientEventResourceProducer = proxyClientEventResourceProducer;
    }

    @PostConstruct
    public void init() {
        registryEntryMap = redissonClient.getMap("RegistryEntries");
    }

    public void registerResourceWithParams(ResourceRegisterParameters resourceRegisterParameters) {

        registerResource(resourceRegisterParameters.getResourceName(),
                resourceRegisterParameters.getCluster(),
                resourceRegisterParameters.getNode(),
                resourceRegisterParameters.getNodeId(),
                resourceRegisterParameters.getPort(),
                resourceRegisterParameters.getPublicServer(),
                resourceRegisterParameters.getPath(),
                resourceRegisterParameters.getContextPath());
    }

    private void registerResource(String name, String cluster, String node, UUID nodeId, int port, PublicServer publicServer,
                                  String path, String contextPath) {

        contextPath = normalizeContextPath(contextPath);      // "" o "/product"
        path = normalizeResourcePath(path);                   // "" o "/graphql" ...

        String clusterEndpoint = joinUrl(STR."\{cluster}:\{port}", contextPath);          // product:8080 + /product
        String resourceEndpoint = joinUrl(clusterEndpoint, path);                              // + /

        String nodeEndpoint = joinUrl(STR."\{node}:\{port}", contextPath);                 // 172.21.0.5:8080 + /product

        String publicEndpoint = null;
        if (publicServer != null) {
            publicEndpoint = joinUrl(STR."\{publicServer.host()}:\{publicServer.port()}", contextPath);
            publicEndpoint = joinUrl(publicEndpoint, path);
        }

        RegistryEntry registryEntry = registryEntryMap.get(name);
        if (registryEntry == null) {
            registryEntry = createRegistryEntry(name, resourceEndpoint);
            ProxyClientEvent event = new ProxyClientEvent(name, ProxyClientEventType.CLIENT_AVAILABLE);
            try {
                proxyClientEventResourceProducer.send(event);
            } catch (Exception ex) {
                log.error(STR."Error sending ProxyClientEvent for resource \{name}", ex);
            }
        }

        registryEntry.setName(name);
        registryEntry.upsertNodeEndpoint(new NodeEndpoint(nodeId, nodeEndpoint));
        registryEntry.setResourceEndpoint(clusterEndpoint);
        registryEntry.setPublicEndpoint(publicEndpoint);
        registryEntry.setDateTime(LocalDateTime.now());
        registryEntryMap.put(name, registryEntry);
        log.info("Registerd resource={}  cluster={} node={}, publicEndpoint={}", name, clusterEndpoint, nodeEndpoint, publicEndpoint);
    }

    private RegistryEntry createRegistryEntry(String name, String endpoint) {
        return registryEntryMap.computeIfAbsent(name, k -> new RegistryEntry(endpoint, name));
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
                .map(RegistryEntry::getResourceEndpoint)              // "product:8080/product"
                .map(c -> EndpointUtils.extractHostPort(c))                          // "product:8080"
                .anyMatch(target::equalsIgnoreCase);
    }


    @Override
    public void removeRegistryEntryNodeEndpoint(String resourceEndpoint, String nodeEndpoint) {
        String targetBaseEndpoint = EndpointUtils.extractServiceBaseFromResource(resourceEndpoint);
        String deadNodeEndpoint = EndpointUtils.normalizeEndpoint(nodeEndpoint);

        Set<String> keysRemove = new HashSet<>();

        registryEntryMap.entrySet().stream()
                .filter(e -> EndpointUtils.extractServiceBaseFromResource(e.getValue().getResourceEndpoint())
                        .equalsIgnoreCase(targetBaseEndpoint))
                .forEach(e -> {
                    RegistryEntry re = e.getValue();

                    re.removeNodeEndpointsIf(ne ->
                            EndpointUtils.normalizeEndpoint(ne.getNodeEndpoint())
                                    .equalsIgnoreCase(deadNodeEndpoint)
                    );

                    if (re.getNodeEndpoints().isEmpty()) {
                        keysRemove.add(e.getKey());
                    } else {
                        registryEntryMap.put(e.getKey(), re);
                    }
                });

        keysRemove.forEach(key -> {
            registryEntryMap.remove(key);
            ProxyClientEvent proxyClientEvent = new ProxyClientEvent(key, ProxyClientEventType.CLIENT_UNAVAILABLE);
            try {
                proxyClientEventResourceProducer.send(proxyClientEvent);
            } catch (Exception e) {
                log.error("Error sending ProxyClientEvent for resource {}: {}", key, e.getMessage());
            }
        });
    }

}
