package io.github.spring.middleware.error;

import io.github.spring.middleware.component.NodeInfoRetriever;
import io.github.spring.middleware.exception.ExceptionUtils;
import io.github.spring.middleware.jms.JmsErrorProducer;
import io.github.spring.middleware.error.api.ErrorRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class ErrorRegister {


    private final JmsErrorProducer jmsErrorProducer;
    private final ApplicationContext applicationContext;
    private final NodeInfoRetriever nodeInfoRetriever;

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
                    .nodeId(nodeInfoRetriever.getNodeId())
                    .hostname(Optional.ofNullable(System.getenv("MY_POD_NAME"))
                            .orElse(nodeInfoRetriever.getAddress()))
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