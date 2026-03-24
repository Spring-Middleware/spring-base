package io.github.spring.middleware.kafka.core.registrar;

import com.fasterxml.jackson.databind.JavaType;

import java.lang.reflect.Method;

public record KafkaListenerMethodMetadata(
        Class<?> beanClass,
        JavaType envelopeType,
        Method method,
        String listenerName) {

}


