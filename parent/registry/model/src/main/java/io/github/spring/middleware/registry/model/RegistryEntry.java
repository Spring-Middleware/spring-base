package io.github.spring.middleware.registry.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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

    public void addNodeEndpoint(NodeEndpoint nodeEndpoint) {
        this.nodeEndpoints.add(nodeEndpoint);
    }

    public Set<NodeEndpoint> getNodeEndpoints() {
        return this.nodeEndpoints;
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

    public void removeNodeEndpoint(String nodeEndpoint) {
        this.nodeEndpoints.removeIf(n -> n.getNodeEndpoint().startsWith(nodeEndpoint));
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
