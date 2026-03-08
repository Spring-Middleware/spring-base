package io.github.spring.middleware.resolver;

import io.github.spring.middleware.error.ErrorDescriptor;
import org.springframework.core.Ordered;

import java.util.Optional;

public interface HttpStatusCodeResolver extends Resolver<ErrorDescriptor, Integer> {

    Optional<Integer> resolve(ErrorDescriptor error);

}
