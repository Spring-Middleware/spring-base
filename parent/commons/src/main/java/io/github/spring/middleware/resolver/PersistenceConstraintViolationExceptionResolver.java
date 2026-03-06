package io.github.spring.middleware.resolver;

import io.github.spring.middleware.error.ConstraintErrorResolver;
import io.github.spring.middleware.error.ErrorDescriptor;
import io.github.spring.middleware.error.ErrorMessage;
import jakarta.persistence.PersistenceException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
@Order(20)
public class PersistenceConstraintViolationExceptionResolver implements ThrowableErrorResolver {

    private final ConstraintErrorResolver errorResolver;

    public PersistenceConstraintViolationExceptionResolver(ConstraintErrorResolver errorResolver) {
        this.errorResolver = errorResolver;
    }

    @Override
    public Optional<ErrorDescriptor> resolve(Throwable t) {
        if (!(t instanceof PersistenceException persistenceException)) {
            return Optional.empty();
        }

        if (!(persistenceException.getCause() instanceof ConstraintViolationException dbViolation)) {
            return Optional.empty();
        }

        ErrorDescriptor baseDescriptor =
                errorResolver.resolveFromDbConstraintName(dbViolation.getConstraintName());

        Map<String, Object> extensions = new LinkedHashMap<>();
        extensions.put("constraintName", dbViolation.getConstraintName());
        extensions.put("sqlState", dbViolation.getSQLState());
        extensions.put("databaseErrorCode", dbViolation.getErrorCode());

        if (baseDescriptor.getExtensions() != null && !baseDescriptor.getExtensions().isEmpty()) {
            extensions.putAll(baseDescriptor.getExtensions());
        }

        ErrorMessage error = new ErrorMessage(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                baseDescriptor.getCode(),
                baseDescriptor.getMessage(),
                extensions
        );

        return Optional.of(error);
    }
}