package io.github.spring.middleware.resolver;

import io.github.spring.middleware.error.ErrorDescriptor;
import org.springframework.core.Ordered;

import java.util.Optional;

public interface HttpStatusCodeResolver extends Ordered {

    Optional<Integer> resolve(ErrorDescriptor error);

    @Override
    default int getOrder() {
        return 0; // prioridad por defecto
    }
}
