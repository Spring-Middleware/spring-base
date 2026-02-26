package com.middleware.jpa.order;

import com.middleware.sort.SortedSearch;
import jakarta.validation.ConstraintValidatorContext;

public interface OrderByValidator {

    boolean orderByValid(SortedSearch s, ConstraintValidatorContext context);
}
