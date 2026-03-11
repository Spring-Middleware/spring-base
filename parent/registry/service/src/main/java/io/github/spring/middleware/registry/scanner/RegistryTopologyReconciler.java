package io.github.spring.middleware.registry.scanner;

import io.github.spring.middleware.component.NodeInfoRetriever;
import io.github.spring.middleware.provider.ServerPortProvider;
import io.github.spring.middleware.registry.model.NodeEndpoint;
import io.github.spring.middleware.registry.model.PublicServer;
import io.github.spring.middleware.registry.model.RegistryEntry;
import io.github.spring.middleware.registry.model.SchemaLocation;
import io.github.spring.middleware.registry.model.SchemaLocationNode;
import io.github.spring.middleware.registry.params.ResourceRegisterParameters;
import io.github.spring.middleware.registry.service.RegistryService;
import io.github.spring.middleware.registry.service.SchemaRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.spring.middleware.registry.util.EndpointUtils.joinUrl;
import static io.github.spring.middleware.registry.util.EndpointUtils.normalizePath;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "middleware.registry-topology-reconciler.enabled", havingValue = "true", matchIfMissing = true)
public class RegistryTopologyReconciler {

    private final SchemaRegistryService schemaRegistryService;
    private final RegistryService registryService;
    private final RegistryTopologyReconcilerProperties props;
    private final ServerPortProvider serverPortProvider;
    private final PublicServer publicServer;
    private final RegistryScannerClient scannerClient;
    private final NodeInfoRetriever nodeInfoRetriever;

    // contador por nodeLocation (ip:port)
    private final ConcurrentHashMap<String, AtomicInteger> noAvailCounters = new ConcurrentHashMap<>();

    @Scheduled(cron = "${middleware.registry-topology-reconciler.cron:*/30 * * * * *}")
    public void tick() {
        log.info("Registry scanner: checking schema nodes & core resources");
        checkSchemaLocations();
        checkRegistryNodeEnpoints();
    }


    private void checkSchemaLocations() {
        // 1) scan schema nodes
        log.info("Scanning schema locations...");
        List<SchemaLocation> schemaLocations = schemaRegistryService.getSchemaLocations();
        if (schemaLocations == null || schemaLocations.isEmpty()) {
            ensureCoreRegistryResources();
            return;
        }

        // aplanamos nodos
        List<NodeRef> nodes = schemaLocations.stream()
                .filter(sl -> sl.getSchemaLocationNodes() != null)
                .flatMap(sl -> sl.getSchemaLocationNodes().stream().map(n -> new NodeRef(sl, n)))
                .toList();

        if (nodes.isEmpty()) {
            ensureCoreRegistryResources();
            return;
        }
        // 2) check alive concurrently, but bounded
        // (sin Reactor también se puede, pero así queda muy limpio)
        Flux.fromIterable(nodes)
                .flatMap(this::checkAlive, props.getConcurrency())
                .doOnNext(this::handleResult)
                .doFinally(sig -> ensureCoreRegistryResources())
                .blockLast();
    }

    private void checkRegistryNodeEnpoints() {
        log.info("Checking registry node endpoints...");
        List<NodeEndpoint> nodeEndpoints = registryService.getRegistryMap().registryMap().values().stream()
                .flatMap(re -> re.getNodeEndpoints().stream())
                .toList();

        nodeEndpoints.stream().filter(n -> !n.getId().equals(nodeInfoRetriever.getNodeId()))
                .forEach(n -> scannerClient.isAlive(getRegistryNodeEndpoint(n.getNodeEndpoint()))
                        .filter(alive -> !alive)
                        .doOnNext(alive -> {
                            log.info("Removing not alive registry node endpoint {} with id {}", n.getNodeEndpoint(), n.getId());
                            registryService.removeRegistryEntryNodeEndpoint(
                                    registryService.getRegistryEnry("registry").getClusterEndpoint(),
                                    n.getNodeEndpoint()
                            );
                        })
                        .subscribe()
                );

    }

    private String getRegistryNodeEndpoint(String nodeEndpoint) {
        String registryClusterEndpoint = nodeEndpoint.substring(0, nodeEndpoint.indexOf("/")); // "product:8080"
        return joinUrl(registryClusterEndpoint, "/registry");
    }


    private Mono<AliveResult> checkAlive(NodeRef ref) {
        String nodeLocation = joinUrl(ref.node().getLocation(), ref.schemaLocation().getContextPath());
        String graphQLPath = joinUrl(nodeLocation, normalizePath(ref.schemaLocation().getPathApi()));

        return scannerClient.isAlive(graphQLPath)
                .map(alive -> alive
                        ? AliveResult.alive(ref)
                        : AliveResult.dead(ref, null))
                .onErrorResume(ex -> Mono.just(AliveResult.dead(ref, ex)));
    }

    private void handleResult(AliveResult r) {
        SchemaLocation schemaLocation = r.ref().schemaLocation();
        SchemaLocationNode node = r.ref().node();

        String namespace = schemaLocation.getNamespace();
        String clusterLocation = schemaLocation.getLocation(); // p.e. "product:8080"
        String nodeLocation = node.getLocation();              // p.e. "192.168.144.5:8080"

        if (r.isAlive()) {
            resetCounter(nodeLocation);

            log.debug("Alive: namespace={}, node={}, pathApi={}", namespace, nodeLocation, schemaLocation.getPathApi());
            schemaRegistryService.refreshNodeLastAliveCheckDate(schemaLocation, nodeLocation);

            // Si existe schemaLocation pero NO existe RegistryEntry del cluster (product:8080),
            // puedes pedirle al nodo que (re)registre resources (como hacía el task viejo)
            if (!registryService.existsClusterHost(clusterLocation)) {
                triggerRegisterResource(nodeLocation);
            }

        } else {
            int fails = incCounter(nodeLocation);
            log.warn("Not alive: namespace={}, node={}, fails={}/{}", namespace, nodeLocation, fails, props.getMaxNoAvail());

            if (fails >= props.getMaxNoAvail()) {
                log.info("Removing dead node: namespace={}, node={}", namespace, nodeLocation);
                removeDeadNode(schemaLocation, node);
            }
        }
    }

    private void removeDeadNode(SchemaLocation schemaLocation, SchemaLocationNode node) {
        String namespace = node.getNamespace();
        String nodeLocation = node.getLocation();

        // 1) remove from schemaLocation
        schemaRegistryService.removeSchemaLocationNode(namespace, nodeLocation);

        // si ya no quedan nodos para ese namespace, borra el SchemaLocation entero
        if (!schemaRegistryService.hasAnyNode(namespace)) {   // implementa esto
            schemaRegistryService.removeSchemaLocation(namespace);
        }

        // 2) remove node endpoint from RegistryEntry cluster (product:8080 -> remove ip:port)
        registryService.removeRegistryEntryNodeEndpoint(schemaLocation.getLocation(), nodeLocation);

        // 3) cleanup counter
        noAvailCounters.remove(nodeLocation);
    }

    private void triggerRegisterResource(String clusterEndpoint) {
        // clusterEndpoint es "product:8080"
        scannerClient.triggerRegisterResource(clusterEndpoint)
                .subscribe(); // fire-and-forget igual que antes
    }

    private void ensureCoreRegistryResources() {
        try {
            Map<String, RegistryEntry> all = registryService.getRegistryMap().registryMap();

            // Ajusta esto a tu modelo real: cluster, publicServer, port, etc.
            // (La idea es: si no existe, registrarlo)
            if (!all.containsKey("registry")) {
                ResourceRegisterParameters resourceRegisterParameters = getResourceRegisterParameters();
                registryService.registerResourceWithParams(resourceRegisterParameters);
            } else {
                RegistryEntry registryEntry = all.get("registry");
                Flux.fromIterable(registryEntry.getNodeEndpoints())
                        .filter(n -> !n.getId().equals(nodeInfoRetriever.getNodeId()))
                        .flatMap(n -> scannerClient.isAlive(n.getNodeEndpoint())
                                .filter(alive -> !alive)
                                .doOnNext(alive -> {
                                    log.info("Removing not alive registry node endpoint {} with id {}", n.getNodeEndpoint(), n.getId());
                                    registryService.removeRegistryEntryNodeEndpoint(
                                            registryEntry.getClusterEndpoint(),
                                            n.getNodeEndpoint()
                                    );
                                })
                        )
                        .then()
                        .subscribe();
            }
        } catch (Exception ex) {
            log.warn("Error ensuring core registry resources", ex);
        }
    }

    private @NonNull ResourceRegisterParameters getResourceRegisterParameters() throws UnknownHostException {
        ResourceRegisterParameters resourceRegisterParameters = new ResourceRegisterParameters();
        resourceRegisterParameters.setResourceName("registry");
        resourceRegisterParameters.setCluster("registry");
        resourceRegisterParameters.setNodeId(nodeInfoRetriever.getNodeId());
        resourceRegisterParameters.setNode(nodeInfoRetriever.getAddress());
        resourceRegisterParameters.setContextPath("/registry");
        resourceRegisterParameters.setPath("/");
        resourceRegisterParameters.setPort(serverPortProvider.getPort());
        resourceRegisterParameters.setPublicServer(publicServer);
        return resourceRegisterParameters;
    }

    private int incCounter(String nodeLocation) {
        return noAvailCounters.computeIfAbsent(nodeLocation, k -> new AtomicInteger(0))
                .incrementAndGet();
    }

    private void resetCounter(String nodeLocation) {
        noAvailCounters.compute(nodeLocation, (k, v) -> {
            if (v == null) return new AtomicInteger(0);
            v.set(0);
            return v;
        });
    }

    // --- records internos ---
    private record NodeRef(SchemaLocation schemaLocation, SchemaLocationNode node) {
    }

    private record AliveResult(NodeRef ref, boolean isAlive, Throwable error) {

        static AliveResult alive(NodeRef ref) {
            return new AliveResult(ref, true, null);
        }

        static AliveResult dead(NodeRef ref, Throwable ex) {
            return new AliveResult(ref, false, ex);
        }
    }

}
