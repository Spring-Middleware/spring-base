package io.github.spring.middleware.jms;

import io.github.spring.middleware.config.HttpHeaderNames;
import io.github.spring.middleware.config.PropertyNames;
import io.github.spring.middleware.filter.InitContextFilter;
import io.github.spring.middleware.rabbitmq.annotations.JmsListener;
import io.github.spring.middleware.rabbitmq.annotations.listener.JmsAllConsumers;
import io.github.spring.middleware.rabbitmq.core.resource.listener.JmsResourceListener;
import org.jboss.logging.MDC;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

@Component
@JmsListener(value = JmsAllConsumers.class)
public class JmsRequestContextConsumerListener implements JmsResourceListener {

    @Override
    public void onBeforeProcessingMessage(Properties properties) {

        Optional.ofNullable(properties.getProperty(PropertyNames.REQUEST_ID)).ifPresent(requestId -> {
            MDC.put(PropertyNames.REQUEST_ID, requestId);
        });
        InitContextFilter.initContext(getContextProperties(properties));
    }

    private Map<String, Object> getContextProperties(Properties properties) {

        HashMap<String, Object> contextProperties = new HashMap<>();
        contextProperties.put(PropertyNames.REQUEST_LOG_ENABLED,
                Optional.ofNullable((String) properties.get(HttpHeaderNames.LogRequestEnabled))
                        .map(s -> Boolean.valueOf(s)).orElse(Boolean.FALSE));
        return contextProperties;
    }
}
