package io.github.spring.middleware.jms;

import io.github.spring.middleware.config.HttpHeaderNames;
import io.github.spring.middleware.config.PropertyNames;
import io.github.spring.middleware.filter.Context;
import com.middleware.jms.annotations.JmsListener;
import com.middleware.jms.annotations.listener.JmsAllProducers;
import com.middleware.jms.core.resource.listener.JmsResourceListener;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Properties;

@Component
@JmsListener(value = JmsAllProducers.class)
public class JmsRequestContextProducerListener implements JmsResourceListener {

    @Override
    public void onBeforeProcessingMessage(Properties properties) {

        Optional.ofNullable(Context.get(PropertyNames.REQUEST_ID)).ifPresent(requestId -> {
            properties.put(PropertyNames.REQUEST_ID, requestId);
        });
        properties.put(HttpHeaderNames.LogRequestEnabled,
                Optional.ofNullable((Boolean) Context.get(PropertyNames.REQUEST_LOG_ENABLED)).map(b -> b.toString())
                        .orElse(Boolean.FALSE.toString()));
    }
}
