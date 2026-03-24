package io.github.spring.middleware.kafka.core.publisher;

import io.github.spring.middleware.kafka.api.interf.KafkaPublisher;

public class RegisteredKafkaPublisher<E, K> {

    private final KafkaPublisher<E, K> publisher;
    private final Class<E> eventClass;
    private final Class<K> keyClass;

    public RegisteredKafkaPublisher(KafkaPublisher<E, K> publisher,
                                    Class<E> eventClass,
                                    Class<K> keyClass) {
        this.publisher = publisher;
        this.eventClass = eventClass;
        this.keyClass = keyClass;
    }

    public KafkaPublisher<E, K> getPublisher() {
        return publisher;
    }

    public Class<E> getEventClass() {
        return eventClass;
    }

    public Class<K> getKeyClass() {
        return keyClass;
    }
}
