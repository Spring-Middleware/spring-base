package io.github.spring.middleware.jpa.order;

import io.github.spring.middleware.sort.SortedSearch;
import jakarta.validation.ConstraintValidatorContext;

public interface OrderByValidator {

    boolean orderByValid(SortedSearch s, ConstraintValidatorContext context);
}
