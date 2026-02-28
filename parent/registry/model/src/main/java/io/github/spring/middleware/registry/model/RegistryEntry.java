package io.github.spring.middleware.registry.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class RegistryEntry implements Serializable {

    private String clusterEndpoint;
    private Set<String> nodeEndpoints;
    private String publicEndpoint;
    private LocalDateTime dateTime;

    public RegistryEntry() {
    }

    public RegistryEntry(String clusterEndpoint) {
        this.clusterEndpoint = clusterEndpoint;
        this.nodeEndpoints = new HashSet<>();
    }

    public void addNodeEndpoint(String nodeEndpoint) {
        this.nodeEndpoints.add(nodeEndpoint);
    }

    public Set<String> getNodeEndpoints() {
        return this.nodeEndpoints;
    }

    public String getClusterEndpoint() {
        return clusterEndpoint;
    }

    public void setClusterEndpoint(String clusterEndpoint) {
        this.clusterEndpoint = clusterEndpoint;
    }

    public void setNodeEndpoints(Set<String> nodeEndpoints) {
        this.nodeEndpoints = nodeEndpoints;
    }

    public String getPublicEndpoint() {
        return publicEndpoint;
    }

    public void setPublicEndpoint(String publicEndpoint) {
        this.publicEndpoint = publicEndpoint;
    }

    public void removeNodeEndpoint(String nodeEndpoint) {
        this.nodeEndpoints.removeIf(name -> name.startsWith(nodeEndpoint));
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public LocalDateTime getDateTime() {
        return this.dateTime;
    }
}
