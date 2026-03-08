package io.github.spring.middleware.resolver;

import java.util.List;
import java.util.Optional;


public abstract class CompositeAbstractResolver<E, R> {

    private final List<? extends Resolver<E, R>> delegates;

    public CompositeAbstractResolver(List<? extends Resolver<E, R>> resolvers) {
        this.delegates = resolvers;
    }

    public Optional<R> resolve(E element) {
        for (Resolver r : delegates) {
            Optional<R> resolved = r.resolve(element);
            if (resolved.isPresent()) return resolved;
        }
        return Optional.empty();
    }
}
