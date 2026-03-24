package io.github.spring.middleware.kafka.core.converter;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.spring.middleware.kafka.api.data.EventEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.SmartMessageConverter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageConverterFactory {

    private final ObjectMapper objectMapper;


    public SmartMessageConverter buildMessageConverter(JavaType envelopeType) {
        return new SmartMessageConverter() {
            @Override
            public Object fromMessage(Message<?> message, Class<?> targetClass) {
                return fromMessage(message, targetClass, null);
            }

            @Override
            public Message<?> toMessage(Object payload, MessageHeaders headers) {
                return toMessage(payload, headers, null);
            }

            @Override
            public Object fromMessage(Message<?> message, Class<?> targetClass, Object conversionHint) {
                Object payload = message.getPayload();
                if (payload == null) {
                    return null;
                }

                if (!(payload instanceof EventEnvelope<?> envelope)) {
                    return objectMapper.convertValue(payload, envelopeType);
                }

                JavaType payloadType = envelopeType.containedType(0);

                Object convertedPayload = objectMapper.convertValue(envelope.getPayload(), payloadType);

                EventEnvelope<Object> convertedEnvelope = new EventEnvelope<>();
                convertedEnvelope.setEventId(envelope.getEventId());
                convertedEnvelope.setEventType(envelope.getEventType());
                convertedEnvelope.setTimestamp(envelope.getTimestamp());
                convertedEnvelope.setTraceId(envelope.getTraceId());
                convertedEnvelope.setPayload(convertedPayload);

                return convertedEnvelope;
            }

            @Override
            public Message<?> toMessage(Object payload, MessageHeaders headers, Object conversionHint) {
                return MessageBuilder
                        .withPayload(payload)
                        .copyHeadersIfAbsent(headers)
                        .build();
            }
        };
    }


}
