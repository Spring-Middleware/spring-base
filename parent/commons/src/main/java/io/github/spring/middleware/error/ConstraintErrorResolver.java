package io.github.spring.middleware.error;

import io.github.spring.middleware.constraint.DbConstraintMapper;
import io.github.spring.middleware.constraint.ValidationConstraintMapper;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;

@Component
public class ConstraintErrorResolver {

    private final List<ValidationConstraintMapper> validationMappers;
    private final List<DbConstraintMapper> dbMappers;

    public ConstraintErrorResolver(List<ValidationConstraintMapper> validationMappers,
                                   List<DbConstraintMapper> dbMappers) {
        this.validationMappers = validationMappers;
        this.dbMappers = dbMappers;
    }

    public ErrorDescriptor resolveFromAnnotation(Class<? extends Annotation> annotationType) {
        return validationMappers.stream()
                .map(m -> m.mapAnnotation(annotationType))
                .flatMap(Optional::stream)
                .findFirst()
                .orElse(new DefaultErrorDescriptor(FrameworkErrorCodes.VALIDATION_ERROR)); // default validation
    }

    public ErrorDescriptor resolveFromDbConstraintName(String constraintName) {
        return dbMappers.stream()
                .map(m -> m.mapConstraintName(constraintName))
                .flatMap(Optional::stream)
                .findFirst()
                .orElse(new DefaultErrorDescriptor(FrameworkErrorCodes.DATABASE_CONSTRAINT_ERROR)); // default database
    }

}
