package io.github.spring.middleware.resolver;

import io.github.spring.middleware.error.DefaultErrorDescriptor;
import io.github.spring.middleware.error.ErrorDescriptor;
import io.github.spring.middleware.error.FrameworkErrorCodes;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(Integer.MAX_VALUE)
public class UnknownErrorResolver implements ThrowableErrorResolver {

    @Override
    public Optional<ErrorDescriptor> resolve(Throwable t) {
        return Optional.of(new DefaultErrorDescriptor(FrameworkErrorCodes.UNKNOWN_ERROR));
    }
}
