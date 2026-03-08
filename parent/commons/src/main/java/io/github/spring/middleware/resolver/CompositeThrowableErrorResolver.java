package io.github.spring.middleware.resolver;

import io.github.spring.middleware.error.ErrorDescriptor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CompositeThrowableErrorResolver extends CompositeAbstractResolver<Throwable, ErrorDescriptor> {

    public CompositeThrowableErrorResolver(List<ThrowableErrorResolver> resolvers) {
        super(resolvers);
    }
}
