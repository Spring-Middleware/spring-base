package io.github.spring.middleware.kafka.core.error;

import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class DefaultKafkaExceptionProvider implements KafkaExceptionProvider {

    @Override
    public Class<? extends Exception>[] getNoRetryableExceptions() {
        Set<Class<? extends Exception>> exceptionsToClassify = new HashSet<>(getSpecifiedNoRetryExceptions());
        exceptionsToClassify.addAll(getSpecifiedNoRetryExceptions());
        exceptionsToClassify.add(IllegalArgumentException.class);
        exceptionsToClassify.add(DeserializationException.class);
        exceptionsToClassify.add(MessageConversionException.class);
        exceptionsToClassify.add(ClassCastException.class);
        return exceptionsToClassify.toArray(new Class[0]);
    }

    protected Set<Class<? extends Exception>> getSpecifiedNoRetryExceptions() {
        return new HashSet<>();
    }
}
