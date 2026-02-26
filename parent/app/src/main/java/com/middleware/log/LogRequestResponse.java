package com.middleware.log;

import com.middleware.config.PropertyNames;
import com.middleware.filter.Context;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LogRequestResponse {

    public void debug(String message) {

        boolean isRequestLog = isLogRequestForced();
        if (log.isDebugEnabled() || isRequestLog) {
            if (log.isDebugEnabled()) {
                log.debug(message);
            } else if (isRequestLog) {
                log.error(message);
            }
        }
    }

    public void warn(String message) {

        boolean isRequestLog = isLogRequestForced();
        if (log.isWarnEnabled() || isRequestLog) {
            if (log.isWarnEnabled()) {
                log.warn(message);
            } else if (isRequestLog) {
                log.error(message);
            }
        }
    }

    public void info(String message) {

        boolean isRequestLog = isLogRequestForced();
        if (log.isInfoEnabled() || isRequestLog) {
            if (log.isInfoEnabled()) {
                log.info(message);
            } else if (isRequestLog) {
                log.error(message);
            }
        }
    }

    public boolean isInfoEnabled() {
        return log.isInfoEnabled() || isLogRequestForced();
    }

    private boolean isLogRequestForced() {

        return Context.get(PropertyNames.REQUEST_LOG_ENABLED);
    }

}
