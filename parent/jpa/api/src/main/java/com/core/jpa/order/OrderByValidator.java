package com.core.jpa.order;

import com.core.sort.SortedSearch;
import jakarta.validation.ConstraintValidatorContext;

public interface OrderByValidator {

    boolean orderByValid(SortedSearch s, ConstraintValidatorContext context);
}
