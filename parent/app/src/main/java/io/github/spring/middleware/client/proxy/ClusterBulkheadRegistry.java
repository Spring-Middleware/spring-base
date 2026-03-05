package io.github.spring.middleware.client.proxy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

@Component
public class ClusterBulkheadRegistry {

    private final ConcurrentHashMap<String, Semaphore> bulkheads = new ConcurrentHashMap<>();
    private final int defaultMaxConcurrent;

    public ClusterBulkheadRegistry(@Value("${middleware.client.defaultMaxConcurrent:30}") int defaultMaxConcurrent) {
        this.defaultMaxConcurrent = Math.max(1, defaultMaxConcurrent);
    }

    public Semaphore getOrCreate(String clusterName, Integer overrideMaxConcurrent) {
        int max = overrideMaxConcurrent != null ? Math.max(1, overrideMaxConcurrent) : defaultMaxConcurrent;

        // crea una sola vez por cluster (por JVM)
        return bulkheads.computeIfAbsent(clusterName, k -> new Semaphore(max));
    }
}
