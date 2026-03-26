package io.github.spring.middleware.kafka.core.autoconfigure;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.spring.middleware.kafka.api.data.EventEnvelope;
import io.github.spring.middleware.kafka.core.converter.MessageConverterFactory;
import io.github.spring.middleware.kafka.core.error.DefaultKafkaExceptionProvider;
import io.github.spring.middleware.kafka.core.error.DefaultMiddlewareKafkaErrorHandlerFactory;
import io.github.spring.middleware.kafka.core.error.KafkaExceptionProvider;
import io.github.spring.middleware.kafka.core.error.MiddlewareKafkaErrorHandlerFactory;
import io.github.spring.middleware.kafka.core.properties.KafkaProperties;
import io.github.spring.middleware.kafka.core.publisher.DefaultKafkaPublisher;
import io.github.spring.middleware.kafka.core.registrar.KafkaListenerMetadataRegistry;
import io.github.spring.middleware.kafka.core.registrar.KafkaListenerRegistrar;
import io.github.spring.middleware.kafka.core.registry.KafkaPublisherRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@AutoConfiguration
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnProperty(prefix = "middleware.kafka", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaAutoConfiguration {


    @Bean
    @ConditionalOnMissingBean
    public KafkaAdmin kafkaAdmin(KafkaProperties properties) {
        Map<String, Object> config = new HashMap<>();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        return new KafkaAdmin(config);
    }


    @Bean(name = "middlewareKafkaTopics")
    @ConditionalOnProperty(
            prefix = "middleware.kafka",
            name = "create-missing-topics",
            havingValue = "true",
            matchIfMissing = true
    )
    public KafkaAdmin.NewTopics middlewareKafkaTopics(KafkaProperties properties, NewTopicFactory newTopicFactory) {
        List<NewTopic> topics = newTopicFactory.buildTopics(properties);
        log.info("Creating Kafka topics: {}", topics.stream().map(NewTopic::name).toList());
        return new KafkaAdmin.NewTopics(topics.toArray(new NewTopic[0]));
    }


    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper kafkaObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    @Bean
    @ConditionalOnMissingBean
    public ProducerFactory<String, Object> kafkaProducerFactory(
            KafkaProperties properties,
            ObjectMapper kafkaObjectMapper
    ) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        DefaultKafkaProducerFactory<String, Object> factory =
                new DefaultKafkaProducerFactory<>(config);

        factory.setValueSerializer(new JsonSerializer<>(kafkaObjectMapper));
        return factory;
    }


    @Bean
    @ConditionalOnMissingBean
    public ConsumerFactory<String, Object> kafkaConsumerFactory(
            KafkaProperties properties,
            ObjectMapper kafkaObjectMapper
    ) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        DefaultKafkaConsumerFactory<String, Object> factory =
                new DefaultKafkaConsumerFactory<>(config);
        factory.setValueDeserializer(new JsonDeserializer<>(kafkaObjectMapper));
        return factory;
    }

    @Bean
    @ConditionalOnMissingBean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
            KafkaTemplate<String, Object> kafkaTemplate,
            KafkaProperties properties
    ) {
        return new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition(
                        record.topic() + properties.getErrorHandling().getDeadLetter().getSuffix(),
                        record.partition()
                )
        );
    }


    @Bean
    @ConditionalOnMissingBean
    public KafkaExceptionProvider kafkaExceptionProvider() {
        return new DefaultKafkaExceptionProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public MiddlewareKafkaErrorHandlerFactory defaultKafkaErrorHandlerFactory(KafkaProperties properties, KafkaExceptionProvider kafkaExceptionProvider, DeadLetterPublishingRecoverer deadLetterPublishingRecoverer) {
        return new DefaultMiddlewareKafkaErrorHandlerFactory(properties, kafkaExceptionProvider, deadLetterPublishingRecoverer);
    }

    @Bean
    @ConditionalOnMissingBean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory, KafkaProperties kafkaProperties, MiddlewareKafkaErrorHandlerFactory errorHandlerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        KafkaProperties.Logging logging = kafkaProperties.getLogging();
        if (logging.isEnabled()) {
            factory.setRecordInterceptor((record, consumer) -> {
                EventEnvelope<?> envelope = (EventEnvelope<?>) record.value();

                Object payload = logging.isLogPayload()
                        ? envelope.getPayload()
                        : "[hidden]";

                Object headers = logging.isLogHeaders()
                        ? record.headers()
                        : "[hidden]";

                log.debug(
                        "Kafka message: topic={}, partition={}, offset={}, key={}, eventId={}, type={}, headers={}, payload={}",
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        record.key(),
                        envelope.getEventId(),
                        envelope.getEventType(),
                        headers,
                        payload
                );
                return record;
            });
        }
        factory.setCommonErrorHandler(errorHandlerFactory.buildErrorHandler());
        return factory;
    }


    @Bean
    @ConditionalOnMissingBean
    public KafkaTemplate<String, Object> kafkaTemplate(
            ProducerFactory<String, Object> producerFactory
    ) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public KafkaPublisherRegistry kafkaPublisherRegistry(KafkaProperties kafkaProperties, KafkaTemplate<String, Object> kafkaTemplate) {
        KafkaPublisherRegistry registry = new KafkaPublisherRegistry();
        kafkaProperties.getPublishers().forEach((name, config) -> {
            registry.registerPublisher(name, new DefaultKafkaPublisher<>(
                    name,
                    kafkaProperties,
                    kafkaTemplate
            ));
        });
        return registry;
    }

    @Bean
    public MessageHandlerMethodFactory kafkaMessageHandlerMethodFactory() {
        DefaultMessageHandlerMethodFactory factory = new DefaultMessageHandlerMethodFactory();
        factory.afterPropertiesSet();
        return factory;
    }

    @Bean
    @ConditionalOnBean(KafkaListenerMetadataRegistry.class)
    public KafkaListenerRegistrar kafkaListenerRegistrar(
            KafkaProperties kafkaProperties,
            KafkaListenerMetadataRegistry metadataRegistry,
            ApplicationContext applicationContext,
            KafkaListenerEndpointRegistry endpointRegistry,
            ConcurrentKafkaListenerContainerFactory<String, Object> containerFactory,
            DefaultMessageHandlerMethodFactory messageHandlerMethodFactory,
            MessageConverterFactory messageConverterFactory
    ) {
        return new KafkaListenerRegistrar(
                kafkaProperties,
                metadataRegistry,
                applicationContext,
                endpointRegistry,
                containerFactory,
                messageHandlerMethodFactory,
                messageConverterFactory
        );
    }

}
