package io.github.spring.middleware.resolver;

import io.github.spring.middleware.error.ErrorDescriptor;

import java.util.Optional;

public interface ThrowableErrorResolver extends Resolver<Throwable, ErrorDescriptor> {
    Optional<ErrorDescriptor> resolve(Throwable t);
}
