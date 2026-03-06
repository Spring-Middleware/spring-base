package io.github.spring.middleware.log;

import io.github.spring.middleware.config.PropertyNames;
import io.github.spring.middleware.filter.Context;
import io.github.spring.middleware.filter.MiddlewareLogProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class LogRequestResponse {

    private final MiddlewareLogProperties middlewareLogProperties;

    public void debug(String message) {

        boolean isRequestLog = isLogForced();
        if (log.isDebugEnabled() || isRequestLog) {
            if (log.isDebugEnabled()) {
                log.debug(message);
            } else if (isRequestLog) {
                log.error(message);
            }
        }
    }

    public void warn(String message) {

        boolean isRequestLog = isLogForced();
        if (log.isWarnEnabled() || isRequestLog) {
            if (log.isWarnEnabled()) {
                log.warn(message);
            } else if (isRequestLog) {
                log.error(message);
            }
        }
    }

    public void info(String message) {

        boolean isRequestLog = isLogForced();
        if (log.isInfoEnabled() || isRequestLog) {
            if (log.isInfoEnabled()) {
                log.info(message);
            } else if (isRequestLog) {
                log.error(message);
            }
        }
    }

    public boolean isInfoEnabled() {
        return log.isInfoEnabled() || isLogForced();
    }

    private boolean isLogForced() {
        return Optional.ofNullable(Context.get(PropertyNames.LOGGING_KEY))
                .map(Object::toString)
                .filter(val -> !val.isBlank())
                .map(val -> val.equalsIgnoreCase(middlewareLogProperties.getApiKey()))
                .orElse(false);
    }

}
