package io.github.spring.middleware.registry.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class RegistryEntry implements Serializable {

    private String name;
    private String clusterEndpoint;
    private Set<NodeEndpoint> nodeEndpoints;
    private String publicEndpoint;
    private LocalDateTime dateTime;

    public RegistryEntry() {
    }

    public RegistryEntry(String clusterEndpoint, String name) {
        this.name = name;
        this.clusterEndpoint = clusterEndpoint;
        this.nodeEndpoints = new HashSet<>();
    }

    public void upsertNodeEndpoint(NodeEndpoint nodeEndpoint) {
        this.nodeEndpoints.remove(nodeEndpoint);
        this.nodeEndpoints.add(nodeEndpoint);
    }

    public void removeNodeEndpointsIf(Predicate<NodeEndpoint> predicate) {
        this.nodeEndpoints.removeIf(predicate);
    }

    public Set<NodeEndpoint> getNodeEndpoints() {
        return Set.copyOf(this.nodeEndpoints);
    }

    public String getClusterEndpoint() {
        return clusterEndpoint;
    }

    public void setClusterEndpoint(String clusterEndpoint) {
        this.clusterEndpoint = clusterEndpoint;
    }

    public void setNodeEndpoints(Set<NodeEndpoint> nodeEndpoints) {
        this.nodeEndpoints = nodeEndpoints;
    }

    public String getPublicEndpoint() {
        return publicEndpoint;
    }

    public void setPublicEndpoint(String publicEndpoint) {
        this.publicEndpoint = publicEndpoint;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public LocalDateTime getDateTime() {
        return this.dateTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
