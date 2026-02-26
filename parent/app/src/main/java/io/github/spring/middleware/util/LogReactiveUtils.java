package io.github.spring.middleware.util;

import io.github.spring.middleware.config.PropertyNames;
import org.slf4j.MDC;
import reactor.core.publisher.Signal;

import java.util.Optional;
import java.util.function.Consumer;

public class LogReactiveUtils {

    public static <T> Consumer<Signal<T>> logOnNext(Consumer<T> logStatement) {

        return signal -> {
            if (!signal.isOnNext())
                return;
            Optional<String> toPutInMdc = signal.getContextView().getOrEmpty(PropertyNames.REQUEST_ID);
            toPutInMdc.ifPresentOrElse(tpim -> {
                        try (MDC.MDCCloseable cMdc = MDC.putCloseable(PropertyNames.REQUEST_ID, tpim)) {
                            logStatement.accept(signal.get());
                        }
                    },
                    () -> logStatement.accept(signal.get()));
        };
    }

    public static Consumer<Signal<?>> logOnError(Consumer<Throwable> errorLogStatement) {
        return signal -> {
            if (!signal.isOnError())
                return;
            Optional<String> toPutInMdc = signal.getContextView().getOrEmpty(PropertyNames.REQUEST_ID);
            toPutInMdc.ifPresentOrElse(tpim -> {
                        try (MDC.MDCCloseable cMdc = MDC.putCloseable(PropertyNames.REQUEST_ID, tpim)) {
                            errorLogStatement.accept(signal.getThrowable());
                        }
                    },
                    () -> errorLogStatement.accept(signal.getThrowable()));
        };
    }

}
