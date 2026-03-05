package io.github.spring.middleware.resolver;

import io.github.spring.middleware.error.ErrorDescriptor;
import io.github.spring.middleware.error.FrameworkErrorProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
@Order(100)
public class FrameworkHttpStatusResolver implements HttpStatusCodeResolver {

    private final Map<String, Integer> statusByCode;

    public FrameworkHttpStatusResolver(FrameworkErrorProperties properties) {
        this.statusByCode = properties.getHttpStatus();
    }

    @Override
    public Optional<Integer> resolve(ErrorDescriptor error) {
        Integer status = statusByCode.get(error.getCode());
        return Optional.ofNullable(status);
    }
}
