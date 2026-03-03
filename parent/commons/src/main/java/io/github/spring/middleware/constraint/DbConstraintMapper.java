package io.github.spring.middleware.constraint;

import io.github.spring.middleware.error.ErrorDescriptor;

import java.util.Optional;

public interface DbConstraintMapper {

    Optional<ErrorDescriptor> mapConstraintName(String constraintName);

}
