package io.github.spring.middleware.resolver;

import io.github.spring.middleware.error.ErrorDescriptor;
import org.springframework.core.Ordered;

import java.util.Optional;

public interface ThrowableErrorResolver extends Ordered {
    Optional<ErrorDescriptor> resolve(Throwable t);

    default int getOrder() {
        return 0;
    }
}
