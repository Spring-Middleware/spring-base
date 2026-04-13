package io.github.spring.middleware.constraint.deflt;

import io.github.spring.middleware.constraint.ValidationConstraintMapper;
import io.github.spring.middleware.error.ConstraintErrorCodes;
import io.github.spring.middleware.error.DefaultErrorDescriptor;
import io.github.spring.middleware.error.ErrorDescriptor;
import io.github.spring.middleware.error.FrameworkErrorCodes;
import org.springframework.stereotype.Service;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class DefaultValidationConstraintMapper implements ValidationConstraintMapper {

    private final Map<Class<? extends Annotation>, ErrorDescriptor> map = new HashMap<>();

    public DefaultValidationConstraintMapper() {
        map.put(jakarta.validation.constraints.NotNull.class, new DefaultErrorDescriptor(ConstraintErrorCodes.NOT_NULL_CONSTRAINT_ERROR));
        map.put(jakarta.validation.constraints.Size.class, new DefaultErrorDescriptor(ConstraintErrorCodes.SIZE_CONSTRAINT_ERROR));
        // etc
    }

    @Override
    public Optional<ErrorDescriptor> mapAnnotation(Class<? extends Annotation> annotationType) {
        return Optional.ofNullable(map.get(annotationType));
    }

    public void putAll(Map<Class<? extends Annotation>, ErrorDescriptor> extra) {
        map.putAll(extra);
    }

}
