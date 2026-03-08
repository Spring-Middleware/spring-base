package io.github.spring.middleware.resolver;

import java.util.Optional;

public interface Resolver<E, R> {

    Optional<R> resolve(E element);
}
