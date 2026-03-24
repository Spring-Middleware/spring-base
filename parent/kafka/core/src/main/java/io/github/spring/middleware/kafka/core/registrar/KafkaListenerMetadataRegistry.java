package io.github.spring.middleware.kafka.core.registrar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KafkaListenerMetadataRegistry {

    private final List<KafkaListenerMethodMetadata> listeners = new ArrayList<>();

    public void register(KafkaListenerMethodMetadata metadata) {
        listeners.add(metadata);
    }

    public List<KafkaListenerMethodMetadata> getListeners() {
        return Collections.unmodifiableList(listeners);
    }
}
