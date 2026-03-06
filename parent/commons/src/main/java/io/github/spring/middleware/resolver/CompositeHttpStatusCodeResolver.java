package io.github.spring.middleware.resolver;

import io.github.spring.middleware.error.ErrorDescriptor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class CompositeHttpStatusCodeResolver {

    private final List<HttpStatusCodeResolver> delegates;

    public CompositeHttpStatusCodeResolver(List<HttpStatusCodeResolver> resolvers) {
        this.delegates = resolvers.stream()
                .sorted(java.util.Comparator.comparingInt(HttpStatusCodeResolver::getOrder))
                .toList();
    }

    public Optional<Integer> resolve(ErrorDescriptor error) {
        for (HttpStatusCodeResolver r : delegates) {
            Optional<Integer> resolved = r.resolve(error);
            if (resolved.isPresent()) return resolved;
        }
        return Optional.empty();
    }
}
