package io.github.spring.middleware.error;

import io.github.spring.middleware.converter.JsonConverter;
import io.github.spring.middleware.error.api.ErrorRecoveryAttemptRequest;
import io.github.spring.middleware.error.api.ErrorRecoveryAttemptView;
import io.github.spring.middleware.error.api.ErrorView;
import io.github.spring.middleware.exception.ExceptionUtils;
import com.middleware.jms.core.resource.JmsResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

@Slf4j
@Service
public class ErrorRecoveryServiceImpl implements ErrorRecoveryService {

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private ErrorClient errorClient;
    private JsonConverter<Properties> propertiesJsonConverter = new JsonConverter<>(Properties.class);

    @Override
    public Flux<Tuple2<UUID, ErrorRecoveryAttemptView>> recoveryErrors(Flux<ErrorView> errorViews) {

        return errorViews.switchIfEmpty(e -> {
            if (log.isDebugEnabled()) {
                log.debug("No errors for try to recevory");
            }
        }).flatMap(errorView -> {
            Object bean = null;
            try {
                bean = applicationContext.getBean(Class.forName(errorView.getClazzName()));
            } catch (Exception ex) {
                log.error("Can't get bean " + errorView.getClazzName());
            }
            return Optional.ofNullable(bean).map(b -> {
                if (JmsResource.class.isAssignableFrom(b.getClass())) {
                    return Mono.zip(Mono.just(errorView.getUuid()), recoveryJMS(errorView));
                } else if (Recoverable.class.isAssignableFrom(b.getClass())) {
                    return Mono.zip(Mono.just(errorView.getUuid()), recover(errorView));
                } else {
                    log.warn("This bean is not recoverable " + errorView.getClazzName());
                    return null;
                }
            }).orElse(null);

        }).switchIfEmpty(e -> {
            if (log.isDebugEnabled()) {
                log.info("Can't create recoveryAttempt");
            }
        });
    }

    private Mono<ErrorRecoveryAttemptView> recoveryJMS(ErrorView errorView) {

        try {
            JmsResource jmsResource = (JmsResource) applicationContext
                    .getBean(Class.forName(errorView.getClazzName()));

            Map<String, String> data = errorView.getData();
            String bodyMessage = data.get("BODY");
            Properties properties = propertiesJsonConverter.toObject(data.get("PROPERTIES"));
            properties.setProperty(JMSProperty.RETRY,
                    String.valueOf(
                            Optional.ofNullable(errorView.getRecoveryAttempts()).map(Collection::size).map(i -> i + 1)
                                    .orElse(1)));
            jmsResource.onMessageRecovery(bodyMessage, properties);
            return setSuccessRecovery(errorView);
        } catch (Exception ex) {
            try {
                log.error("Error recovering error " + errorView.getUuid());
                return setFailedRecovery(errorView, ex);
            } catch (Exception ex2) {
                log.error("Error setting failed recovery " + errorView.getUuid());
                return null;
            }
        }
    }

    private Mono<ErrorRecoveryAttemptView> recover(ErrorView errorView) {

        try {
            Recoverable recoverable = (Recoverable) applicationContext.getBean(Class.forName(errorView.getClazzName()));
            recoverable.recover(errorView);
            return setSuccessRecovery(errorView);
        } catch (Exception ex) {
            try {
                log.error("Error recovering error " + errorView.getUuid());
                return setFailedRecovery(errorView, ex);
            } catch (Exception ex2) {
                log.error("Error setting failed recovery " + errorView.getUuid());
                return null;
            }
        }
    }

    private Mono<ErrorRecoveryAttemptView> setFailedRecovery(ErrorView errorView,
                                                             Exception exception) throws Exception {

        return errorClient.setErrorRecoveryAttempt(errorView.getUuid(),
                ErrorRecoveryAttemptRequest.builder().errorRecoveryMessage(
                        ExceptionUtils.getNotNullMessage(exception)).errorId(errorView.getUuid()).recovered(false)
                        .dateTime(
                                LocalDateTime.now()).build());
    }

    private Mono<ErrorRecoveryAttemptView> setSuccessRecovery(ErrorView errorView) throws Exception {

        return errorClient.setErrorRecoveryAttempt(errorView.getUuid(),
                ErrorRecoveryAttemptRequest.builder().recovered(true).dateTime(LocalDateTime.now()).build());
    }
}
