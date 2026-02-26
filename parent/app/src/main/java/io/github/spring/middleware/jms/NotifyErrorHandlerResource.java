package io.github.spring.middleware.jms;

import io.github.spring.middleware.converter.JsonConverter;
import io.github.spring.middleware.error.ErrorRegister;
import io.github.spring.middleware.jms.annotations.NotifyErrorHandler;
import io.github.spring.middleware.rabbitmq.annotations.JmsErrorHandler;
import io.github.spring.middleware.rabbitmq.core.resource.handler.ErrorHandlerContext;
import io.github.spring.middleware.rabbitmq.core.resource.handler.JmsResourceErrorHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Slf4j
@Component
@JmsErrorHandler(value = NotifyErrorHandler.class)
public class NotifyErrorHandlerResource<T> implements JmsResourceErrorHandler<T> {

    @Autowired
    private ErrorRegister errorRegister;

    private JsonConverter<Properties> propertiesJsonConverter = new JsonConverter<>(Properties.class);

    public void handleError(ErrorHandlerContext<T> errorHandlerContext) {

        String jsonRequest = null;
        try {
            Object message = errorHandlerContext.getT();
            String jmsMessage = null;
            if (!(message instanceof String)) {
                JsonConverter jsonConverter = new JsonConverter(message.getClass());
                jmsMessage = jsonConverter.toString(message);
            } else {
                jmsMessage = (String) message;
            }

            Map<String, String> data = new HashMap<>();
            data.put("BODY", jmsMessage);
            data.put("PROPERTIES", propertiesJsonConverter.toString(errorHandlerContext.getProperties()));

            errorRegister.registryError(errorHandlerContext.getException(),
                    errorHandlerContext.getJmsResourceType().name(), errorHandlerContext.getClazz().getName(),
                    data, null);
        } catch (Exception ex) {
            log.error("Can't register jms error " + jsonRequest);
        }
    }

}
