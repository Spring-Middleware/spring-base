package io.github.spring.middleware.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MiddlewareContract {

    String name();

    String enabled() default "true";

    MiddlewareContractConnection connection() default @MiddlewareContractConnection;

    MiddlewareCircuitBreaker circuitBreaker() default @MiddlewareCircuitBreaker;

    String security() default "NONE";
}
