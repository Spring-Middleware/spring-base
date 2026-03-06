package io.github.spring.middleware.resolver;

import io.github.spring.middleware.error.ConstraintErrorResolver;
import io.github.spring.middleware.error.ErrorDescriptor;
import io.github.spring.middleware.error.ErrorMessage;
import io.github.spring.middleware.error.FrameworkErrorCodes;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.util.*;

@Component
@Order(10)
public class ConstraintViolationExceptionResolver implements ThrowableErrorResolver {

    private static final ErrorDescriptor VALIDATION_ERROR_CODE = FrameworkErrorCodes.VALIDATION_ERROR;

    private final ConstraintErrorResolver errorResolver;

    public ConstraintViolationExceptionResolver(ConstraintErrorResolver errorResolver) {
        this.errorResolver = errorResolver;
    }

    @Override
    public Optional<ErrorDescriptor> resolve(Throwable t) {
        if (!(t instanceof ConstraintViolationException ex)) {
            return Optional.empty();
        }

        List<Map<String, Object>> violations = ex.getConstraintViolations().stream()
                .sorted(Comparator.comparing(v -> String.valueOf(v.getPropertyPath())))
                .map(this::toViolation)
                .toList();

        Map<String, Object> extensions = new LinkedHashMap<>();
        extensions.put("violations", violations);
        extensions.put("violationCount", violations.size());

        return Optional.of(new ErrorMessage(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                VALIDATION_ERROR_CODE.getCode(),
                buildTopLevelMessage(violations),
                extensions
        ));
    }

    private Map<String, Object> toViolation(ConstraintViolation<?> violation) {
        Annotation annotation = violation.getConstraintDescriptor().getAnnotation();
        Class<? extends Annotation> annoType = annotation.annotationType();

        ErrorDescriptor baseDescriptor = errorResolver.resolveFromAnnotation(annoType);

        String path = String.valueOf(violation.getPropertyPath());

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("code", baseDescriptor.getCode());
        detail.put("message", violation.getMessage());
        detail.put("field", extractFieldName(path));
        detail.put("path", path);
        detail.put("rejectedValue", violation.getInvalidValue());
        detail.put("constraint", annoType.getSimpleName());
        detail.put("attributes", extractUsefulAttributes(violation));

        if (baseDescriptor.getExtensions() != null && !baseDescriptor.getExtensions().isEmpty()) {
            detail.put("extensions", baseDescriptor.getExtensions());
        }

        return detail;
    }

    private Map<String, Object> extractUsefulAttributes(ConstraintViolation<?> violation) {
        Map<String, Object> raw = violation.getConstraintDescriptor().getAttributes();
        Map<String, Object> filtered = new LinkedHashMap<>();

        raw.forEach((key, value) -> {
            if (!"groups".equals(key) && !"payload".equals(key) && !"message".equals(key)) {
                filtered.put(key, value);
            }
        });

        return filtered;
    }

    private String buildTopLevelMessage(List<Map<String, Object>> violations) {
        return violations.stream()
                .map(v -> v.get("field") + ": " + v.get("message"))
                .distinct()
                .reduce((a, b) -> a + "; " + b)
                .orElse(VALIDATION_ERROR_CODE.getMessage());
    }

    private String extractFieldName(String propertyPath) {
        if (StringUtils.isBlank(propertyPath)) {
            return propertyPath;
        }

        String[] parts = propertyPath.split("\\.");
        return parts[parts.length - 1];
    }
}