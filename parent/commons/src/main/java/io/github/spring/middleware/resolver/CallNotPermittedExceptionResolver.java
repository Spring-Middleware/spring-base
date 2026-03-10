package io.github.spring.middleware.resolver;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.spring.middleware.error.ErrorDescriptor;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static io.github.spring.middleware.error.FrameworkErrorCodes.CALL_NOT_PERMITTED;

@Component
public class CallNotPermittedExceptionResolver implements ThrowableErrorResolver {

    @Override
    public Optional<ErrorDescriptor> resolve(Throwable t) {
        if (!(t instanceof CallNotPermittedException ex)) {
            return Optional.empty();
        }
        return Optional.of(CALL_NOT_PERMITTED);
    }
}
