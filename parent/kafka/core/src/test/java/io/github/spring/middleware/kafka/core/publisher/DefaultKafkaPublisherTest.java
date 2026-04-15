package io.github.spring.middleware.kafka.core.publisher;

import io.github.spring.middleware.config.PropertyNames;
import io.github.spring.middleware.kafka.api.annotations.EventType;
import io.github.spring.middleware.kafka.api.data.EventEnvelope;
import io.github.spring.middleware.kafka.api.data.PublishResult;
import io.github.spring.middleware.kafka.core.exception.KafkaErrorCodes;
import io.github.spring.middleware.kafka.core.exception.KafkaException;
import io.github.spring.middleware.kafka.core.properties.KafkaProperties;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultKafkaPublisherTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafkaTemplate = (KafkaTemplate<String, Object>) mock(KafkaTemplate.class);

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void publishWithKey_whenEventIsNull_throwsKafkaException() {
        KafkaProperties properties = new KafkaProperties();
        properties.setPublishers(new HashMap<>());

        DefaultKafkaPublisher<String, String> publisher =
                new DefaultKafkaPublisher<>("order-created", properties, kafkaTemplate);

        assertThatThrownBy(() -> publisher.publishWithKey(null, "key"))
                .isInstanceOf(KafkaException.class)
                .hasMessage("Event to publish cannot be null")
                .hasFieldOrPropertyWithValue("errorCode", KafkaErrorCodes.KAFKA_EVENT_NULL);

    }

    @Test
    void publishWithoutKey_whenEventIsNull_throwsKafkaException() {
        KafkaProperties properties = new KafkaProperties();
        properties.setPublishers(new HashMap<>());

        DefaultKafkaPublisher<String, String> publisher =
                new DefaultKafkaPublisher<>("order-created", properties, kafkaTemplate);

        assertThatThrownBy(() -> publisher.publish(null))
                .isInstanceOf(KafkaException.class)
                .hasMessage("Event to publish cannot be null")
                .hasFieldOrPropertyWithValue("errorCode", KafkaErrorCodes.KAFKA_EVENT_NULL);
    }

    @Test
    void publishWhenPublisherNotConfigured_throwsKafkaException() {
        KafkaProperties properties = new KafkaProperties();
        properties.setPublishers(new HashMap<>()); // empty, no publisher configured

        DefaultKafkaPublisher<String, String> publisher =
                new DefaultKafkaPublisher<>("order-created", properties, kafkaTemplate);

        assertThatThrownBy(() -> publisher.publishWithKey("event", "key"))
                .isInstanceOf(KafkaException.class)
                .hasMessage("Kafka publisher order-created is not configured")
                .hasFieldOrPropertyWithValue("errorCode", KafkaErrorCodes.KAFKA_PUBLISHER_NOT_FOUND);
    }

    @Test
    void publishWhenTopicEmpty_throwsKafkaException() {
        KafkaProperties properties = buildPropertiesWithTopic("order-created", "");

        DefaultKafkaPublisher<String, String> publisher =
                new DefaultKafkaPublisher<>("order-created", properties, kafkaTemplate);

        assertThatThrownBy(() -> publisher.publishWithKey("event", "key"))
                .isInstanceOf(KafkaException.class)
                .hasMessage("Kafka topic for publisher order-created is not configured")
                .hasFieldOrPropertyWithValue("errorCode", KafkaErrorCodes.KAFKA_TOPIC_NOT_CONFIGURED);
    }

    @Test
    void publishWhenTopicBlank_throwsKafkaException() {
        KafkaProperties properties = buildPropertiesWithTopic("order-created", "   ");

        DefaultKafkaPublisher<String, String> publisher =
                new DefaultKafkaPublisher<>("order-created", properties, kafkaTemplate);

        assertThatThrownBy(() -> publisher.publishWithKey("event", "key"))
                .isInstanceOf(KafkaException.class)
                .hasMessage("Kafka topic for publisher order-created is not configured")
                .hasFieldOrPropertyWithValue("errorCode", KafkaErrorCodes.KAFKA_TOPIC_NOT_CONFIGURED);
    }

    @Test
    void publishWithKey_buildsCorrectEnvelope_andResult_usesMdcTraceId_andSimpleClassNameAsEventType()
            throws ExecutionException, InterruptedException {

        KafkaProperties properties = buildPropertiesWithTopic("order-created", "orders.created.v1");

        DefaultKafkaPublisher<TestEvent, String> publisher =
                new DefaultKafkaPublisher<>("order-created", properties, kafkaTemplate);

        String traceId = UUID.randomUUID().toString();
        MDC.put(PropertyNames.REQUEST_ID, traceId);

        TestEvent event = new TestEvent("id-1");
        String topic = "orders.created.v1";
        String key = "my-key";

        SendResult<String, Object> sendResult = buildSendResult(topic, 1, 42L);

        when(kafkaTemplate.send(eq(topic), eq(key), any(EventEnvelope.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        PublishResult<TestEvent, String> result = publisher.publishWithKey(event, key).get();

        assertThat(result.getKey()).isEqualTo(key);
        assertThat(result.getTopic()).isEqualTo(topic);
        assertThat(result.getPartition()).isEqualTo(1);
        assertThat(result.getOffset()).isEqualTo(42L);

        @SuppressWarnings("unchecked")
        EventEnvelope<TestEvent> envelope = result.getEvent();
        assertThat(envelope).isNotNull();
        assertThat(envelope.getPayload()).isEqualTo(event);
        assertThat(envelope.getEventId()).isNotNull();
        assertThat(envelope.getEventId()).isNotEmpty();
        assertThat(envelope.getTimestamp()).isNotNull();
        assertThat(envelope.getTraceId()).isEqualTo(traceId);
        assertThat(envelope.getEventType()).isEqualTo("TestEvent");
    }

    @Test
    void publishWithAnnotatedEvent_usesEventTypeAnnotationValue()
            throws ExecutionException, InterruptedException {

        KafkaProperties properties = buildPropertiesWithTopic("order-created", "orders.created.v1");

        DefaultKafkaPublisher<AnnotatedEvent, String> publisher =
                new DefaultKafkaPublisher<>("order-created", properties, kafkaTemplate);

        AnnotatedEvent event = new AnnotatedEvent("id-2");
        String topic = "orders.created.v1";
        String key = "my-key";

        SendResult<String, Object> sendResult = buildSendResult(topic, 0, 10L);

        when(kafkaTemplate.send(eq(topic), eq(key), any(EventEnvelope.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        PublishResult<AnnotatedEvent, String> result = publisher.publishWithKey(event, key).get();

        assertThat(result.getEvent().getEventType()).isEqualTo("order-created");
    }

    @Test
    void publishWhenNoTraceIdInMdc_generatesNewTraceId()
            throws ExecutionException, InterruptedException {

        KafkaProperties properties = buildPropertiesWithTopic("order-created", "orders.created.v1");

        DefaultKafkaPublisher<TestEvent, String> publisher =
                new DefaultKafkaPublisher<>("order-created", properties, kafkaTemplate);

        MDC.remove(PropertyNames.REQUEST_ID);

        TestEvent event = new TestEvent("id-3");
        String topic = "orders.created.v1";

        SendResult<String, Object> sendResult = buildSendResult(topic, 0, 1L);

        when(kafkaTemplate.send(eq(topic), any(EventEnvelope.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        PublishResult<TestEvent, String> result = publisher.publish(event).get();

        assertThat(result.getEvent().getTraceId()).isNotNull();
        assertThat(result.getEvent().getTraceId()).isNotEmpty();
    }

    @Test
    void publishWithoutKey_setsNullKeyInResult()
            throws ExecutionException, InterruptedException {

        KafkaProperties properties = buildPropertiesWithTopic("order-created", "orders.created.v1");

        DefaultKafkaPublisher<TestEvent, String> publisher =
                new DefaultKafkaPublisher<>("order-created", properties, kafkaTemplate);

        TestEvent event = new TestEvent("id-4");
        String topic = "orders.created.v1";

        SendResult<String, Object> sendResult = buildSendResult(topic, 2, 100L);

        when(kafkaTemplate.send(eq(topic), any(EventEnvelope.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        PublishResult<TestEvent, String> result = publisher.publish(event).get();

        assertThat(result.getKey()).isNull();
        assertThat(result.getTopic()).isEqualTo(topic);
        assertThat(result.getPartition()).isEqualTo(2);
        assertThat(result.getOffset()).isEqualTo(100L);
    }

    private KafkaProperties buildPropertiesWithTopic(String publisherName, String topic) {
        KafkaProperties properties = new KafkaProperties();
        Map<String, KafkaProperties.Publisher> publishers = new HashMap<>();
        KafkaProperties.Publisher publisher = new KafkaProperties.Publisher();
        publisher.setTopic(topic);
        publishers.put(publisherName, publisher);
        properties.setPublishers(publishers);
        return properties;
    }

    private SendResult<String, Object> buildSendResult(String topic, int partition, long offset) {
        RecordMetadata metadata = new RecordMetadata(
                new org.apache.kafka.common.TopicPartition(topic, partition),
                0L,
                offset,
                System.currentTimeMillis(),
                0L,
                0,
                0
        );
        return new SendResult<>(null, metadata);
    }

    static class TestEvent {
        private final String id;

        TestEvent(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    @EventType("order-created")
    static class AnnotatedEvent {
        private final String id;

        AnnotatedEvent(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }
}

