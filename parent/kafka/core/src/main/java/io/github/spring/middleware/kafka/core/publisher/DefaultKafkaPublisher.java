package io.github.spring.middleware.kafka.core.publisher;

import io.github.spring.middleware.config.PropertyNames;
import io.github.spring.middleware.kafka.api.annotations.EventType;
import io.github.spring.middleware.kafka.api.data.EventEnvelope;
import io.github.spring.middleware.kafka.api.data.PublishResult;
import io.github.spring.middleware.kafka.api.interf.KafkaPublisher;
import io.github.spring.middleware.kafka.core.exception.KafkaErrorCodes;
import io.github.spring.middleware.kafka.core.exception.KafkaException;
import io.github.spring.middleware.kafka.core.properties.KafkaProperties;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DefaultKafkaPublisher<T, K> implements KafkaPublisher<T, K> {

    private final String publisherName;
    private final KafkaProperties kafkaProperties;
    private final KafkaTemplate<K, Object> kafkaTemplate;

    public DefaultKafkaPublisher(String publisherName,
                                 KafkaProperties kafkaProperties,
                                 KafkaTemplate<K, Object> kafkaTemplate) {
        this.publisherName = publisherName;
        this.kafkaProperties = kafkaProperties;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public CompletableFuture<PublishResult<T, K>> publishWithKey(T event, K key) {
        validateEvent(event);

        String topic = resolveTopic();
        EventEnvelope<T> envelope = buildEnvelope(event);

        return kafkaTemplate.send(topic, key, envelope)
                .thenApply(sendResult ->
                        createPublishResult(envelope, key, sendResult)
                );
    }

    @Override
    public CompletableFuture<PublishResult<T, K>> publish(T event) {
        validateEvent(event);

        String topic = resolveTopic();
        EventEnvelope<T> envelope = buildEnvelope(event);

        return kafkaTemplate.send(topic, envelope)
                .thenApply(sendResult ->
                        createPublishResult(envelope, null, sendResult)
                );
    }

    private PublishResult<T, K> createPublishResult(EventEnvelope<T> envelope, K key, SendResult sendResult) {
        PublishResult<T, K> result = new PublishResult<>();
        result.setKey(key);
        result.setEvent(envelope);
        result.setTopic(sendResult.getRecordMetadata().topic());
        result.setPartition(sendResult.getRecordMetadata().partition());
        result.setOffset(sendResult.getRecordMetadata().offset());
        return result;
    }


    private void validateEvent(T event) {
        if (event == null) {
            throw new KafkaException(
                    KafkaErrorCodes.KAFKA_EVENT_NULL,
                    "Event to publish cannot be null"
            );
        }
    }

    private String resolveTopic() {
        KafkaProperties.Publisher publisher = kafkaProperties.getPublishers().get(publisherName);

        if (publisher == null) {
            throw new KafkaException(
                    KafkaErrorCodes.KAFKA_PUBLISHER_NOT_FOUND,
                    STR."Kafka publisher \{publisherName} is not configured"
            );
        }

        String topic = publisher.getTopic();

        if (topic == null || topic.isBlank()) {
            throw new KafkaException(
                    KafkaErrorCodes.KAFKA_TOPIC_NOT_CONFIGURED,
                    STR."Kafka topic for publisher \{publisherName} is not configured"
            );
        }

        return topic;
    }

    private EventEnvelope<T> buildEnvelope(T event) {
        EventEnvelope<T> envelope = new EventEnvelope<>();
        envelope.setEventId(UUID.randomUUID().toString());
        envelope.setEventType(resolveEventType(event));
        envelope.setTimestamp(Instant.now());
        envelope.setTraceId(resolveTraceId());
        envelope.setPayload(event);
        return envelope;
    }

    private String resolveEventType(T event) {
        EventType annotation = event.getClass().getAnnotation(EventType.class);
        return annotation != null && annotation.value() != null && !annotation.value().isBlank()
                ? annotation.value()
                : event.getClass().getSimpleName();
    }

    private String resolveTraceId() {
        return Optional.ofNullable(MDC.get(PropertyNames.REQUEST_ID))
                .filter(traceId -> !traceId.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString());
    }
}

