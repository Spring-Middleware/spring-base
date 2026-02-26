package io.github.spring.middleware.jpa.annotations;

import io.github.spring.middleware.jpa.order.OrderByValidatorRetriever;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {OrderByValidatorRetriever.class})
public @interface OrderByValid {

    String message() default "Invalid order by";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
