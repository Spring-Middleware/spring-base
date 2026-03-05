package io.github.spring.middleware.resolver;

import io.github.spring.middleware.error.ErrorDescriptor;
import io.github.spring.middleware.error.RemoteError;
import io.github.spring.middleware.exception.ServiceException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(Integer.MAX_VALUE)
public class DefaultHttpStatusCodeResolver implements HttpStatusCodeResolver {

    @Override
    public Optional<Integer> resolve(ErrorDescriptor error) {
        if (error instanceof ServiceException se) return Optional.of(se.getHttpStatus().value());
        if (error instanceof RemoteError re) return Optional.of(re.getHttpStatusCode());
        return Optional.empty();
    }
}
