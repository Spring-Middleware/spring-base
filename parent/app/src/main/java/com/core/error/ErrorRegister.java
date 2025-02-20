package com.core.error;

import com.core.exception.ExceptionUtils;
import com.core.jms.JmsErrorProducer;
import com.core.error.api.ErrorRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class ErrorRegister {

    @Autowired
    private JmsErrorProducer jmsErrorProducer;
    @Autowired
    private ApplicationContext applicationContext;

    public void registryErrorAsync(Throwable exception, String operationName, String clazzName,
                                   Map<String, String> data,
                                   Boolean recoverable) {

        CompletableFuture.runAsync(() -> registryError(exception, operationName, clazzName, data, recoverable));
    }

    public void registryError(Throwable exception, String operationName, String clazzName, Map<String, String> data,
                              Boolean recoverable) {

        try {
            ErrorRequest errorRequest = ErrorRequest.builder()
                    .uuid(UUID.randomUUID())
                    .errorMessage(ExceptionUtils.getNotNullMessage(exception))
                    .data(data)
                    .recoverable(recoverable)
                    .serviceName(applicationContext.getId())
                    .hostname(Optional.ofNullable(System.getenv("MY_POD_NAME"))
                            .orElse(InetAddress.getLocalHost().getHostName()))
                    .stackTrace(ExceptionUtils.getStackTrace(exception, 10))
                    .dateTime(LocalDateTime.now())
                    .clazzName(clazzName)
                    .operationName(operationName)
                    .build();

            jmsErrorProducer.send(errorRequest);

        } catch (Exception ex) {
            log.error("Can't register error " + exception.getMessage());
        }
    }

    public void registryError(Throwable exception, String operationName, String clazzName, String message,
                              Boolean recoverable) {
        Map<String,String> data = new HashMap<>();
        data.put("BODY", message);
        registryError(exception, operationName, clazzName, data, recoverable);

    }

}