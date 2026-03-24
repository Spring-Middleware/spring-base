package io.github.spring.middleware.kafka.core.registry;

import io.github.spring.middleware.kafka.api.interf.KafkaPublisher;
import io.github.spring.middleware.kafka.core.exception.KafkaErrorCodes;
import io.github.spring.middleware.kafka.core.exception.KafkaException;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.spring.middleware.kafka.core.exception.KafkaErrorCodes.KAFKA_PUBLISHER_ALREADY_REGISTERED;

public class KafkaPublisherRegistry {

    private final Map<String, KafkaPublisher<?, ?>> publishers = new ConcurrentHashMap<>();

    public void registerPublisher(String name, KafkaPublisher<?, ?> publisher) {
        if (publishers.containsKey(name)) {
            throw new KafkaException(KAFKA_PUBLISHER_ALREADY_REGISTERED, STR."Publisher with name '\{name}' is already registered.");
        }
        publishers.put(name, publisher);
    }

    @SuppressWarnings("unchecked")
    public <E, K> KafkaPublisher<E, K> getPublisher(String name) {
        return (KafkaPublisher<E, K>) Optional.ofNullable(publishers.get(name))
                .orElseThrow(() -> new KafkaException(
                        KafkaErrorCodes.KAFKA_PUBLISHER_NOT_FOUND,
                        STR."Kafka publisher with name '\{name}' not found in registry."
                ));
    }
}
