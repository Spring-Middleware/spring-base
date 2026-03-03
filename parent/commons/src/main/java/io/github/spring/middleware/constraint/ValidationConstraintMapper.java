package io.github.spring.middleware.constraint;

import io.github.spring.middleware.error.ErrorDescriptor;

import java.lang.annotation.Annotation;
import java.util.Optional;

public interface ValidationConstraintMapper {

    Optional<ErrorDescriptor> mapAnnotation(Class<? extends Annotation> annotationType);
}
