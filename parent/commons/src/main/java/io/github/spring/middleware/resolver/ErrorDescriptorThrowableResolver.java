package io.github.spring.middleware.resolver;

import io.github.spring.middleware.error.ErrorDescriptor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(0)
public class ErrorDescriptorThrowableResolver implements ThrowableErrorResolver {

    @Override
    public Optional<ErrorDescriptor> resolve(Throwable t) {
        if (t instanceof ErrorDescriptor ed) {
            return Optional.of(ed);
        }
        return Optional.empty();
    }
}
