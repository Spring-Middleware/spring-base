package io.github.spring.middleware.kafka.core.registrar;

import io.github.spring.middleware.kafka.core.converter.MessageConverterFactory;
import io.github.spring.middleware.kafka.core.exception.KafkaErrorCodes;
import io.github.spring.middleware.kafka.core.exception.KafkaException;
import io.github.spring.middleware.kafka.core.properties.KafkaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.config.MethodKafkaListenerEndpoint;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(KafkaProperties.class)
public class KafkaListenerRegistrar implements SmartInitializingSingleton {

    private final KafkaProperties kafkaProperties;
    private final KafkaListenerMetadataRegistry metadataRegistry;
    private final ApplicationContext applicationContext;
    private final KafkaListenerEndpointRegistry endpointRegistry;
    private final ConcurrentKafkaListenerContainerFactory<String, Object> containerFactory;
    private final DefaultMessageHandlerMethodFactory messageHandlerMethodFactory;
    private final MessageConverterFactory messageConverterFactory;


    @Override
    public void afterSingletonsInstantiated() {
        for (KafkaListenerMethodMetadata metadata : metadataRegistry.getListeners()) {
            registerListener(metadata);
        }
    }

    private void registerListener(KafkaListenerMethodMetadata metadata) {
        KafkaProperties.Subscriber subscriber =
                kafkaProperties.getSubscribers().get(metadata.listenerName());

        if (subscriber == null) {
            throw new KafkaException(
                    KafkaErrorCodes.KAFKA_SUBSCRIBER_NOT_FOUND,
                    STR."Kafka subscriber \{metadata.listenerName()} is not configured"
            );
        }

        Object bean = applicationContext.getBean(metadata.beanClass());

        MethodKafkaListenerEndpoint<Object, Object> endpoint = new MethodKafkaListenerEndpoint<>();
        endpoint.setId(metadata.listenerName());
        endpoint.setBean(bean);
        endpoint.setMethod(metadata.method());
        endpoint.setGroupId(subscriber.getGroupId());
        endpoint.setTopics(subscriber.getTopic());
        endpoint.setConcurrency(subscriber.getConcurrency());
        endpoint.setMessagingConverter(messageConverterFactory.buildMessageConverter(metadata.envelopeType()));
        endpoint.setMessageHandlerMethodFactory(messageHandlerMethodFactory);
        endpointRegistry.registerListenerContainer(endpoint, containerFactory, true);
        log.info("Registered Kafka listener for bean: {} and method: {} with topic: {} and groupId: {}",
                metadata.beanClass().getName(),
                metadata.method().getName(),
                subscriber.getTopic(),
                subscriber.getGroupId());
    }
}
