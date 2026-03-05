package io.github.spring.middleware.resolver;

import io.github.spring.middleware.error.ErrorDescriptor;
import io.github.spring.middleware.error.RemoteError;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(10)
public class RemoteErrorResolver implements ThrowableErrorResolver {

    @Override
    public Optional<ErrorDescriptor> resolve(Throwable t) {
        if (t instanceof RemoteError re) {
            return Optional.of(re);
        }
        return Optional.empty();
    }
}
